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

package models

import com.google.inject.{ ImplementedBy, Inject }
import model.EntityType.withName
import model.faceted.search.{ Aggregation, Facets, MetaDataBucket, NodeBucket }
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms

import scala.collection.JavaConversions._

@ImplementedBy(classOf[ESAggregateService])
trait AggregateService {

  def aggregate(facets: Facets, aggregateKey: String, size: Int, include: List[String], exclude: List[String])(index: String): Aggregation
  def aggregateAll(facets: Facets, size: Int, excludeKeys: List[String])(index: String): List[Aggregation]

  def aggregateEntities(facets: Facets, size: Int, include: List[Long], exclude: List[Long])(index: String): Aggregation
  def aggregateEntitiesByType(facets: Facets, etype: String, size: Int, include: List[Long], exclude: List[Long])(index: String): Aggregation

  def aggregateKeywords(facets: Facets, size: Int, include: List[String])(index: String): Aggregation
}

class ESAggregateService @Inject() (clientService: SearchClientService, utils: ESRequestUtils) extends AggregateService {

  def aggregate(facets: Facets, aggregateKey: String, size: Int, include: List[String], exclude: List[String])(index: String): Aggregation = {
    val field = aggregationToField(index)(aggregateKey)
    termAggregate(facets, Map(aggregateKey -> (field, size)), include, Nil, 1, index).head
  }

  def aggregateAll(facets: Facets, size: Int, excludeKeys: List[String])(index: String): List[Aggregation] = {
    val validAggregations = aggregationToField(index).filterKeys(!excludeKeys.contains(_))
    termAggregate(facets, validAggregations.map { case (k, v) => (k, (v, size)) }, Nil, Nil, 1, index)
  }

  def aggregateEntities(facets: Facets, size: Int, include: List[Long], exclude: List[Long])(index: String): Aggregation = {
    // TODO Improve
    val field = aggregationToField(index)(utils.entityIdsField._1)
    termAggregate(facets, Map(utils.entityIdsField._1 -> (field, size)), include.map(_.toString), exclude.map(_.toString), 1, index).head
  }

  override def aggregateEntitiesByType(facets: Facets, etype: String, size: Int, include: List[Long], exclude: List[Long])(index: String): Aggregation = {
    val agg = Map(utils.entityIdsField._1 -> (utils.entityTypeToField(withName(etype)), size))
    termAggregate(facets, agg, include.map(_.toString), exclude.map(_.toString), 1, index).head
  }

  override def aggregateKeywords(facets: Facets, size: Int, include: List[String])(index: String): Aggregation = {
    // TODO Duplicate code
    val field = aggregationToField(index)(utils.keywordsField._1)
    termAggregate(facets, Map(utils.keywordsField._1 -> (field, size)), include, Nil, 1, index).head
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
