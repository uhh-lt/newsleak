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

import scala.collection.mutable.ListBuffer
import com.google.inject.{ ImplementedBy, Inject }
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality
import util.es.ESRequestUtils
import models.{ Facets, NodeBucket, MetaDataBucket, Aggregation, Relationship }

import scala.collection.JavaConversions._

@ImplementedBy(classOf[ESNetworkService])
trait NetworkService {

  def createNetwork(facets: Facets, nodeFraction: Map[String, Int], exclude: List[Long])(index: String): (List[NodeBucket], List[Relationship])
  def induceNetwork(facets: Facets, currentNetwork: List[Long], nodes: List[Long])(index: String): (List[NodeBucket], List[Relationship])

  def getNeighbors(facets: Facets, entityId: Long, size: Int, exclude: List[Long])(index: String): List[NodeBucket]
  def getNeighborCountsPerType(facets: Facets, entityId: Long)(index: String): Aggregation
  def getEdgeKeywords(facets: Facets, source: Long, dest: Long, numTerms: Int)(index: String): Aggregation
}

class ESNetworkService @Inject() (clientService: SearchClientService, aggregateService: AggregateService, utils: ESRequestUtils) extends NetworkService {

  override def createNetwork(
    facets: Facets,
    nodeFraction: Map[String, Int],
    exclude: List[Long]
  )(index: String): (List[NodeBucket], List[Relationship]) = {
    val buckets = nodeFraction.flatMap {
      case (t, size) =>
        aggregateService.aggregateEntitiesByType(facets, t, size, List(), exclude)(index).buckets
    }.toList

    val rels = induceRelationships(facets, buckets.collect { case NodeBucket(id, _) => id }, index)
    (buckets.collect { case a @ NodeBucket(_, _) => a }, rels)
  }

  private def induceRelationships(facets: Facets, nodes: List[Long], index: String): List[Relationship] = {
    val visitedList = ListBuffer[Long]()
    val rels = nodes.flatMap { source =>
      visitedList.add(source)
      val rest = nodes.filter(!visitedList.contains(_))
      rest.flatMap { dest => getRelationship(facets, source, dest, index) }
    }
    rels
  }

  private def getRelationship(facets: Facets, source: Long, dest: Long, index: String): Option[Relationship] = {
    val t = List(source, dest)
    val agg = aggregateService.aggregateEntities(facets.withEntities(t), 2, t, Nil)(index)
    agg match {
      // No edge between both since their frequency is zero
      case Aggregation(_, NodeBucket(nodeA, 0) :: NodeBucket(nodeB, 0) :: Nil) =>
        None
      case Aggregation(_, NodeBucket(nodeA, freqA) :: NodeBucket(nodeB, freqB) :: Nil) =>
        // freqA and freqB are the same since we query for docs containing both
        Some(Relationship(nodeA, nodeB, freqA))
      case _ => None
    }
  }

  override def induceNetwork(facets: Facets, currentNetwork: List[Long], nodes: List[Long])(index: String): (List[NodeBucket], List[Relationship]) = {
    val buckets = aggregateService.aggregateEntities(facets, nodes.length, nodes, Nil)(index).buckets.collect { case a @ NodeBucket(_, _) => a }
    // Fetch relationships between new nodes
    val inBetweenRels = induceRelationships(facets, nodes, index)
    // Fetch relationships between new nodes and current network
    val connectingRels = nodes.flatMap { source =>
      currentNetwork.flatMap { dest => getRelationship(facets, source, dest, index) }
    }
    (buckets, inBetweenRels ++ connectingRels)
  }

  override def getNeighbors(facets: Facets, entityId: Long, size: Int, exclude: List[Long])(index: String): List[NodeBucket] = {
    val res = aggregateService.aggregateEntities(facets.withEntities(List(entityId)), size, List(), exclude)(index)
    res.buckets.collect { case a @ NodeBucket(_, _) => a }
  }

  override def getNeighborCountsPerType(facets: Facets, entityId: Long)(index: String): Aggregation = {
    // Add entity id as entities filter in order to receive documents where both co-occur
    val neighborFacets = facets.withEntities(List(entityId))
    cardinalityAggregate(neighborFacets, 0, index)
  }

  // TODO: Maybe move to aggregateService
  private def cardinalityAggregate(facets: Facets, documentSize: Int, index: String): Aggregation = {
    val requestBuilder = utils.createSearchRequest(facets, documentSize, index, clientService)
    // Add neighbor aggregation for each NE type
    utils.entityTypeToField.foreach {
      case (eType, f) =>
        val aggregation = AggregationBuilders
          .cardinality(eType.toString)
          .field(f)

        requestBuilder.addAggregation(aggregation)
    }
    val response = utils.executeRequest(requestBuilder)
    // Parse result
    val buckets = utils.entityTypeToField.map {
      case (eType, _) =>
        val agg: Cardinality = response.getAggregations.get(eType.toString)
        MetaDataBucket(eType.toString, agg.getValue)
    }.toList

    Aggregation("neighbors", buckets)
  }

  override def getEdgeKeywords(facets: Facets, source: Long, dest: Long, numTerms: Int)(index: String): Aggregation = {
    // Only consider documents where the two entities occur
    aggregateService.aggregateKeywords(facets.withEntities(List(source, dest)), numTerms, Nil, Nil)(index)
  }
}
