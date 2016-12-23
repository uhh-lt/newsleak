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
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms
// scalastyle:off
import scala.collection.JavaConversions._
// scalastyle:on
import models.{ Facets, Aggregation, MetaDataBucket, NodeBucket }
import util.es.ESRequestUtils

/**
 * Defines common data access methods for generating aggregated data based on a search query.
 *
 * The search query is given as [[models.Facets]]. The service builds analytic information over a set of documents and
 * provides the result as [[models.Aggregation]]. Such a [[models.Aggregation]] consists of a list of [[models.Bucket]],
 * where each bucket is associated with a key and the number of documents that match the given search query. The trait
 * further supports mechanisms to exclude and include specific keys.
 *
 * The trait is implemented by [[models.services.ESAggregateService]], which uses an elasticsearch index as backend.
 */
@ImplementedBy(classOf[ESAggregateService])
trait AggregateService {

  /**
   * Creates multiple buckets - one per unique value that is associated with the given key.
   *
   * The following snippet will create five buckets. Each bucket represents one of the five
   * most occurring recipient names that are associated with a document and match the empty filter:
   * {{{
   *   aggregate(Facets.empty, "Recipient_names", 5, Nil, Nil)("enron")
   * }}}
   *
   * Use [[models.services.DocumentService#getMetadataKeys]] in order to retrieve the available
   * aggregation keys for the underlying collection.
   *
   * @param facets the search query.
   * @param aggregateKey the key that belongs to the aggregated values.
   * @param size the number of unique [[models.MetaDataBucket]] to create.
   * @param include a list of values to filter the result. The result will only contain the [[models.MetaDataBucket]] associated
   * with one of the keys given in this list. The size parameter is ignored when given a non empty include list.
   * @param exclude a list of values that should be excluded from the result. The result will contain no [[models.MetaDataBucket]]
   * associated with one of the keys given in this list.
   * @param index the data source index or database name to query.
   * @return an instance of [[models.Aggregation]] with '''size''' [[models.MetaDataBucket]] matching the given filters and using the aggregationKey.
   */
  def aggregate(facets: Facets, aggregateKey: String, size: Int, include: List[String], exclude: List[String])(index: String): Aggregation

  /**
   * Creates multiple aggregations - one for each metadata from the underlying collection.
   *
   * @param facets the search query.
   * @param size the number of unique [[models.MetaDataBucket]] to create. The size is the same for all aggregations.
   * @param keyExclusion a list of metadata keys that should be excluded from the aggregation.
   * @param index the data source index or database name to query.
   * @return a list of [[models.Aggregation]]. Each with '''size''' [[models.MetaDataBucket]] matching the given filters.
   */
  def aggregateAll(facets: Facets, size: Int, keyExclusion: List[String])(index: String): List[Aggregation]

  /**
   * Creates multiple buckets - one per unique entity.
   *
   * Each document has a list of co-occurring entities, which is used to build this aggregation.
   *
   * The following snippet will create five buckets. Each bucket represents one of the five
   * most occurring entity ids that are associated with a document and match the empty filter:
   * {{{
   *   aggregateEntities(Facets.empty, 5, Nil, Nil)("enron")
   * }}}
   *
   * @param facets the search query.
   * @param size the number of unique [[models.NodeBucket]] to create.
   * @param include a list of values to filter the result. The result will only contain the [[models.NodeBucket]] associated
   * with one of the keys given in this list. The size parameter is ignored when given a non empty include list.
   * @param exclude a list of values that should be excluded from the result. The result will contain no [[models.NodeBucket]]
   * associated with one of the keys given in this list.
   * @param index the data source index or database name to query.
   * @return an instance of [[models.Aggregation]] with '''size''' [[models.NodeBucket]] representing most occurring entities in the
   * underlying collection.
   */
  def aggregateEntities(facets: Facets, size: Int, include: List[Long], exclude: List[Long])(index: String): Aggregation

  /**
   * Creates multiple buckets - one per unique entity. The aggregation only considers entities belonging to the given type.
   *
   * @param facets the search query.
   * @param etype the entity type to filter for.
   * @param size the number of unique [[models.NodeBucket]] to create.
   * @param include a list of values to filter the result. The result will only contain the [[models.NodeBucket]] associated
   * with one of the keys given in this list. The size parameter is ignored when given a non empty include list.
   * @param exclude a list of values that should be excluded from the result. The result will contain no [[models.NodeBucket]]
   * associated with one of the keys given in this list.
   * @param index the data source index or database name to query.
   * @return an instance of [[models.Aggregation]] with '''size''' [[models.NodeBucket]] representing most occurring entities of the given type
   * in the underlying collection.
   */
  def aggregateEntitiesByType(facets: Facets, etype: String, size: Int, include: List[Long], exclude: List[Long])(index: String): Aggregation

  /**
   * Creates multiple buckets - one per unique keyword.
   *
   * Each document has a list important keywords, which is used to build this aggregation.
   *
   * @param facets the search query.
   * @param size the number of unique [[models.MetaDataBucket]] to create.
   * @param include a list of values to filter the result. The result will only contain the [[models.MetaDataBucket]] associated
   * with one of the keys given in this list. The size parameter is ignored when given a non empty include list.
   * @param exclude a list of values that should be excluded from the result. The result will contain no [[models.MetaDataBucket]]
   * associated with one of the keys given in this list.
   * @param index the data source index or database name to query.
   * @return an instance of  [[models.Aggregation]] with '''size''' [[models.MetaDataBucket]] representing most occurring entities in the
   * underlying collection.
   */
  def aggregateKeywords(facets: Facets, size: Int, include: List[String], exclude: List[String])(index: String): Aggregation
}

/**
 * Implementation of [[models.services.AggregateService]] using an elasticsearch index as backend.
 *
 * @param clientService the elasticsearch client.
 * @param utils common helper to issue elasticsearch queries.
 *
 * @see See [[https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket.html ES documentation]]
 * for more information.
 */
class ESAggregateService @Inject() (clientService: SearchClientService, utils: ESRequestUtils) extends AggregateService {

  /** @inheritdoc */
  def aggregate(facets: Facets, aggregateKey: String, size: Int, include: List[String], exclude: List[String])(index: String): Aggregation = {
    val field = aggregationToField(index)(aggregateKey)
    termAggregate(facets, Map(aggregateKey -> (field, size)), include, exclude, 1, index).head
  }

  /** @inheritdoc */
  def aggregateAll(facets: Facets, size: Int, keyExclusion: List[String])(index: String): List[Aggregation] = {
    val validAggregations = aggregationToField(index).filterKeys(!keyExclusion.contains(_))
    termAggregate(facets, validAggregations.map { case (k, v) => (k, (v, size)) }, Nil, Nil, 1, index)
  }

  /** @inheritdoc */
  def aggregateEntities(facets: Facets, size: Int, include: List[Long], exclude: List[Long])(index: String): Aggregation = {
    aggregate(facets, utils.entityIdsField._1, size, include.map(_.toString), exclude.map(_.toString))(index)
  }

  /** @inheritdoc */
  override def aggregateEntitiesByType(facets: Facets, etype: String, size: Int, include: List[Long], exclude: List[Long])(index: String): Aggregation = {
    val agg = Map(utils.entityIdsField._1 -> (utils.convertEntityTypeToField(etype), size))
    termAggregate(facets, agg, include.map(_.toString), exclude.map(_.toString), 1, index).head
  }

  /** @inheritdoc */
  override def aggregateKeywords(facets: Facets, size: Int, include: List[String], exclude: List[String])(index: String): Aggregation = {
    aggregate(facets, utils.keywordsField._1, size, include, exclude)(index)
  }

  private def termAggregate(
    facets: Facets,
    aggs: Map[String, (String, Int)],
    include: List[String],
    exclude: List[String],
    thresholdDocCount: Int,
    index: String
  ): List[Aggregation] = {

    var requestBuilder = utils.createSearchRequest(facets, thresholdDocCount, index, clientService)

    val nonEmptyAggs = aggs.collect {
      // Ignore aggregations with zero size since ES returns all indexed types in this case.
      // We do not want this behaviour and return Aggregations with empty buckets instead.
      case (entry @ (k, (v, size))) if size != 0 =>
        // Default order is bucket size desc
        val agg = AggregationBuilders.terms(k)
          .field(v)
          .size(size)
          // Include empty buckets
          .minDocCount(thresholdDocCount)

        // Apply filter to the aggregation request
        val includeAggOpt = if (include.isEmpty) agg else agg.include(include.toArray)
        val excludeAggOpt = if (exclude.isEmpty) includeAggOpt else includeAggOpt.exclude(exclude.toArray)
        requestBuilder = requestBuilder.addAggregation(excludeAggOpt)
        entry
    }
    val response = utils.executeRequest(requestBuilder)
    // There is no need to call shutdown, since this node is the only one in the cluster.
    parseResult(response, nonEmptyAggs, include) ++ aggs.collect { case ((k, (_, 0))) => Aggregation(k, List()) }
  }

  private def parseResult(response: SearchResponse, aggregations: Map[String, (String, Int)], filters: List[String]): List[Aggregation] = {
    val res = aggregations.collect {
      // Create node bucket for entities
      case (k, (v, s)) if k == utils.entityIdsField._1 =>
        val agg: Terms = response.getAggregations.get(k)
        val buckets = agg.getBuckets.collect {
          // If include filter is given don't add zero count entries (will be post processed)
          case (b) if filters.nonEmpty && filters.contains(b.getKeyAsString) => NodeBucket(b.getKeyAsNumber.longValue(), b.getDocCount)
          case (b) if filters.isEmpty => NodeBucket(b.getKeyAsNumber.longValue(), b.getDocCount)
        }.toList
        // We need to add missing zero buckets for entities filters manually,
        // because aggregation is not able to process long ids with zero buckets
        val addedBuckets = buckets.map(_.id)
        val zeroEntities = filters.filterNot(s => addedBuckets.contains(s.toInt))

        val resBuckets = if (response.getHits.getTotalHits == 0) List() else buckets
        Aggregation(k, resBuckets ::: zeroEntities.map(s => NodeBucket(s.toInt, 0)))
      case (k, (v, s)) =>
        val agg: Terms = response.getAggregations.get(k)
        val buckets = agg.getBuckets.map(b => MetaDataBucket(b.getKeyAsString, b.getDocCount)).toList

        val resBuckets = if (response.getHits.getTotalHits == 0) buckets.filter(b => filters.contains(b.key)) else buckets
        Aggregation(k, resBuckets)
    }
    res.toList
  }

  private val aggregationToField: (String) => Map[String, String] = {
    (index: String) => aggregationFields(index).map(k => k -> s"$k.raw").toMap ++ Map(utils.keywordsField, utils.entityIdsField)
  }

  private def aggregationFields(index: String): List[String] = {
    val res = clientService.client.admin().indices().getMappings(new GetMappingsRequest().indices(index)).get()
    val mapping = res.mappings().get(index)
    val terms = mapping.flatMap { m =>
      val source = m.value.sourceAsMap()
      val properties = source.get("properties").asInstanceOf[java.util.LinkedHashMap[String, java.util.LinkedHashMap[String, String]]]
      properties.keySet()
    }
    terms.toList
  }
}
