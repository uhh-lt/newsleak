/*
 * Copyright (C) 2016 Language Technology Group and Interactive Graphics Systems Group, Technische Universität Darmstadt, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package models.services

import com.google.inject.{ ImplementedBy, Inject }
import models.{ KeywordAggregation, KeywordNetwork, Tag }
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality

import scala.collection.mutable.ListBuffer
import scalikejdbc._
// scalastyle:off
import scala.collection.JavaConversions._
// scalastyle:on
import models.{ Aggregation, Facets, MetaDataBucket, KeyTerm, NodeBucket, KeywordRelationship }
import util.es.{ ESRequestUtils }

/**
 * Defines common method for creating and extending co-occurrence networks given a search query.
 *
 * The trait is implemented by [[models.services.ESKeywordNetworkService]].
 */
@ImplementedBy(classOf[ESKeywordNetworkService])
trait KeywordNetworkService {

  /**
   * Returns a co-occurrence network matching the given search query.
   *
   * @param facets the search query.
   * @param nodeFraction a map linking from entity types to the number of nodes to request for each type.
   * @param exclude a list of entity ids that should be excluded from the result. The result will contain no [[models.NodeBucket]]
   * associated with one of the ids given in this list.
   * @param index the data source index or database name to query.
   * @return a [[models.KeywordNetwork]] consisting of the nodes and relationships of the created co-occurrence network.
   */
  def createNetworkKeyword(facets: Facets, nodeFraction: Map[String, Int], exclude: List[String])(index: String): KeywordNetwork

  /**
   * Adds new nodes to the current network matching the given search query.
   *
   * The method induces relationships and nodes for the given entitiesToAdd considering the already present network i.e.
   * it induces relationships between the new nodes and the current network and between the new nodes. It does not
   * provide nodes for the current network nor relationships between those nodes.
   *
   * @param facets        the search query.
   * @param currentNetwork the keyword terms of the current network.
   * @param nodes         new keywords to be added to the network.
   * @param index         the data source index or database name to query.
   * @return a [[models.KeywordNetwork]] consisting of the nodes and relationships of the created co-occurrence network.
   */
  def induceNetworkKeyword(facets: Facets, currentNetwork: List[String], nodes: List[String])(index: String): KeywordNetwork

  /**
   * Returns entities co-occurring with the given entity matching the search query.
   *
   * @param facets the search query.
   * @param entityId the entity id.
   * @param size the number of neighbors to fetch.
   * @param exclude a list of entity ids that should be excluded from the result. The result will contain no [[models.NodeBucket]]
   * associated with one of the ids given in this list.
   * @param index the data source index or database name to query.
   * @return a list of [[models.NodeBucket]] co-occurring with the given entity.
   */
  def getNeighborsKeyword(facets: Facets, entityId: Long, size: Int, exclude: List[Long])(index: String): List[NodeBucket]

  /**
   * Accumulates the number of entities that fall in a certain entity type and co-occur with the given entity.
   *
   * The result will contain ''n'' different buckets with ''n'' representing the distinct entity types for the underlying collection.
   *
   * @param facets the search query.
   * @param entityId the entity id.
   * @param index the data source index or database name to query.
   * @return a map linking from the unique entity type to the number of neighbors of that type.
   */
  def getNeighborCountsPerTypeKeyword(facets: Facets, entityId: Long)(index: String): Map[String, Int]

  /**
   * Selects all tags from the DB
   *
   * @return a list of all Tags that are stored in the DB
   */
  def getAllTags()(index: String): List[Tag]

  /**
   * Toggles if tags should be calculated
   *
   * @param set the boolean flag
   */
  def toggleTags(set: Boolean): Unit

  /**
   * Removes a blacklisted keyword from the DB
   *
   * @param blacklistedKeywords the blacklisted keyword
   * @param index the ES index
   */
  def undoBlacklistingKeywords(blacklistedKeywords: List[String])(index: String): Unit

  /**
   * Sets the tag keyword relations calculated by ES js
   *
   * @param tag the tag
   * @param keywords the list of keywords
   */
  def setTagKeywordRelation(tag: String, keywords: List[KeyTerm]): Unit

  /**
   * Resets the tag keyword relation for new calculation
   */
  def resetTagKeywordRelation(): Unit

}

/**
 * Implementation of [[models.services.KeywordNetworkService]] using an elasticsearch index as backend.
 *
 * @param clientService    the elasticsearch client.
 * @param aggregateService the aggregation service.
 * @param entityService    the entity service.
 * @param utils            common helper to issue elasticsearch queries.
 * @param networkService  the network service
 */
class ESKeywordNetworkService @Inject() (
    clientService: SearchClientService,
    aggregateService: AggregateService,
    entityService: EntityService,
    utils: ESRequestUtils,
    networkService: NetworkService
) extends KeywordNetworkService {

  private val db = (index: String) => NamedDB(Symbol(index))
  var addTags = false
  var keywordTagRelation: ListBuffer[(String, List[KeyTerm])] = ListBuffer()
  var calculatedKeywords: ListBuffer[KeyTerm] = ListBuffer()
  var calculatedKeywordRelations: ListBuffer[KeywordRelationship] = ListBuffer()

  /** @inheritdoc */
  //noinspection ScalaStyle
  override def createNetworkKeyword(
    facets: Facets,
    nodeFraction: Map[String, Int],
    exclude: List[String]
  )(index: String): KeywordNetwork = {

    val keywordGraphNodes: ListBuffer[KeyTerm] = ListBuffer()
    val searchableTerms: ListBuffer[String] = ListBuffer()

    if (addTags) {
      val resultTags = keywordTagRelation.toList

      for (tag <- resultTags) {
        keywordGraphNodes.append(KeyTerm(tag._1, -1, "TAG"))
        for (key <- tag._2) {
          calculatedKeywordRelations.append(KeywordRelationship(tag._1, key.term, key.score))
        }
      }

      for (keyword <- calculatedKeywords) {
        if (!exclude.contains(keyword.term)) {
          keywordGraphNodes.append(keyword)
          searchableTerms.append(keyword.term)
        }
      }

    } else {

      val size = nodeFraction.collect({
        case (t, size) => size
      }).toList(0)

      val keywords = aggregateService.keywordAggregate(facets, utils.keywordsField._1, size,
        List(), exclude)(index).keywords.distinct

      for (keyword <- keywords) {
        if (!exclude.contains(keyword.term)) {
          keywordGraphNodes.append(keyword)
          searchableTerms.append(keyword.term)
        }
      }

      calculatedKeywords.clear()
      keywords.foreach(x => calculatedKeywords.append(x))

      val rels = induceRelationshipsKeyword(facets, searchableTerms.toList, index)

      calculatedKeywordRelations.clear()
      rels.foreach(x => calculatedKeywordRelations.append(x))
    }

    KeywordNetwork(keywordGraphNodes.toList, calculatedKeywordRelations.toList)
  }

  /** @inheritdoc */
  private def induceRelationshipsKeyword(facets: Facets, nodes: List[String], index: String): List[KeywordRelationship] = {
    val visitedList = ListBuffer[String]()
    val rels = nodes.flatMap { source =>
      visitedList.add(source)
      val rest = nodes.filter(!visitedList.contains(_))
      rest.flatMap { dest => getRelationshipKeyword(facets, source, dest, index) }
    }
    rels
  }

  /** @inheritdoc */
  private def getRelationshipKeyword(facets: Facets, source: String, dest: String, index: String): Option[KeywordRelationship] = {
    val t = List(source, dest)
    // val agg = aggregateService.aggregateEntities(facets.withEntities(t), 2, t, Nil)(index)
    val agg = aggregateService.keywordAggregate(facets, utils.keywordsField._1, 2, t, Nil)(index)
    agg match {
      // No edge between both since their frequency is zero
      case KeywordAggregation(_, KeyTerm(nodeA, 0, "KEYWORD") :: KeyTerm(nodeB, 0, "KEYWORD") :: Nil) =>
        None
      case KeywordAggregation(_, KeyTerm(nodeA, freqA, "KEYWORD") :: KeyTerm(nodeB, freqB, "KEYWORD") :: Nil) =>
        // freqA and freqB are the same since we query for docs containing both
        Some(KeywordRelationship(nodeA, nodeB, freqA))
      case _ => None
    }
  }

  /** @inheritdoc */
  //noinspection ScalaStyle
  override def induceNetworkKeyword(facets: Facets, currentNetwork: List[String], nodes: List[String])(index: String): KeywordNetwork = {
    val buckets = aggregateService.aggregateKeywords(facets, nodes.length, nodes, Nil)(index).buckets.collect { case MetaDataBucket(key, occurrence) => KeyTerm(key, occurrence, "KEYWORD") }
    // Fetch relationships between new nodes
    val inBetweenRels = induceRelationshipsKeyword(facets, nodes, index)
    // Fetch relationships between new nodes and current network
    val connectingRels = nodes.flatMap { source =>
      currentNetwork.flatMap { dest => getRelationshipKeyword(facets, source, dest, index) }
    }

    KeywordNetwork(buckets, inBetweenRels ++ connectingRels)

  }

  /** @inheritdoc */
  override def getNeighborsKeyword(facets: Facets, entityId: Long, size: Int, exclude: List[Long])(index: String): List[NodeBucket] = {
    val res = aggregateService.aggregateEntities(facets.withEntities(List(entityId)), size, List(), exclude)(index)
    res.buckets.collect { case a @ NodeBucket(_, _) => a }
  }

  /** @inheritdoc */
  override def getNeighborCountsPerTypeKeyword(facets: Facets, entityId: Long)(index: String): Map[String, Int] = {
    // Add entity id as entities filter in order to receive documents where both co-occur
    val neighborFacets = facets.withEntities(List(entityId))
    val res = cardinalityAggregateKeyword(neighborFacets, 0, index)
    res.buckets.collect { case MetaDataBucket(term, count) => (term, count.toInt) }.toMap
  }

  // TODO: Maybe move to aggregateService
  private def cardinalityAggregateKeyword(facets: Facets, documentSize: Int, index: String): Aggregation = {
    val requestBuilder = utils.createSearchRequest(facets, documentSize, index, clientService)
    // Add neighbor aggregation for each NE type
    val types = entityService.getTypes()(index).keys
    types.foreach { t =>
      val aggregation = AggregationBuilders
        .cardinality(t)
        .field(utils.convertEntityTypeToField(t))

      requestBuilder.addAggregation(aggregation)
    }
    val response = utils.executeRequest(requestBuilder)
    // Parse result
    val buckets = types.map { t =>
      val agg: Cardinality = response.getAggregations.get(t)
      MetaDataBucket(t, agg.getValue)
    }.toList

    Aggregation("neighbors", buckets)
  }

  /** @inheritdoc */
  override def getAllTags()(index: String): List[Tag] = db(index).readOnly { implicit session =>
    sql"""SELECT t.id, t.documentid, l.label FROM tags t
          INNER JOIN labels AS l ON l.id = t.labelid"""
      .map(Tag(_)).list().apply()
  }

  /**
   * stores all keywords in database to add blacklist field
   * @param keywords
   * @param index
   */
  private def storeKeywordsInDB(keywords: List[KeyTerm])(index: String): Unit = db(index).localTx { implicit session =>
    // TODO with occurence
    for (keyword <- keywords) {
      var keyterm: String = ""
      var numResults: List[String] = List()
      Option(keyword.term) match {
        case Some(value) =>
          keyterm = keyword.term
          val selectString = sql"""SELECT term FROM terms WHERE $keyterm = term"""
          numResults = selectString.map(_.toString).list().apply()

          if (numResults.length == 0) {
            sql"""INSERT INTO terms(term, frequency, isblacklisted) values ($keyterm, 1, false)"""
              .update().apply()
          }
        case None => ;
      }
    }
  }

  /** @inheritdoc */
  override def toggleTags(set: Boolean): Unit = {
    addTags = set
  }

  /** @inheritdoc */
  override def undoBlacklistingKeywords(blacklistedKeywords: List[String])(index: String) = db(index).localTx { implicit session =>
    for (bk <- blacklistedKeywords) {
      sql"DELETE FROM terms where term = ${bk}".update().apply()
    }
  }

  /** @inheritdoc */
  override def setTagKeywordRelation(tag: String, keywords: List[KeyTerm]): Unit = {
    keywordTagRelation.append((tag, keywords))
  }

  /** @inheritdoc */
  override def resetTagKeywordRelation(): Unit = {
    keywordTagRelation.clear()
  }
}
