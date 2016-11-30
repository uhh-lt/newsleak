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
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.histogram.{ DateHistogramInterval, Histogram }
import org.elasticsearch.search.aggregations.metrics.max.Max
import org.elasticsearch.search.aggregations.metrics.min.Min
import org.joda.time.LocalDateTime
// scalastyle:off
import scala.collection.JavaConversions._
// scalastyle:on
import models.{ Facets, LoD, Aggregation, MetaDataBucket, Bucket }
import util.DateUtils
import util.es.ESRequestUtils

/**
 * Defines common methods for creating timeline instances based on document creation dates and time expressions matching
 * a search query.
 *
 * The trait is implemented by [[models.services.ESTimelineService]].
 */
@ImplementedBy(classOf[ESTimelineService])
trait TimelineService {

  /**
   * Creates a timeline mapping from dates to the number of documents per date based on the document creation date.
   *
   * The timeline supports different drill-down steps called "level of detail (lod)". In the most coarse grained level
   * the result maps from grouped decades to number of documents that fall in the decade i.e. 1990-1999: 10 docs,
   * 2000-2009: 20 docs, etc.
   *
   * Similar, the most fine grained level maps from days of a month to the number of documents that fall in a certain day.
   *
   * @param facets the search query.
   * @param levelOfDetail the timeline level.
   * @param index the data source index or database name to query.
   * @return  an instance of [[models.Aggregation]] consisting of [[models.MetaDataBucket]], each representing a time unit with the
   * number of documents for this unit.
   */
  def createTimeline(facets: Facets, levelOfDetail: LoD.Value)(index: String): Aggregation

  /**
   * Creates a timeline mapping from dates to the number of documents per date based on time expression that occur in the
   * document content.
   *
   * @param facets the search query.
   * @param levelOfDetail the timeline level.
   * @param index the data source index or database name to query.
   * @return an instance of [[models.Aggregation]] consisting of [[models.MetaDataBucket]], each representing a time unit with the
   * number of documents for this unit.
   *
   * @see the method [[models.services.TimelineService#createTimeline]].
   */
  def createTimeExpressionTimeline(facets: Facets, levelOfDetail: LoD.Value)(index: String): Aggregation
}

/**
 * Implementation of [[models.services.TimelineService]] using an elasticsearch index as backend.
 *
 * @param clientService the elasticsearch client.
 * @param dateUtils common heper for date and time operations.
 * @param utils common helper to issue elasticsearch queries.
 */
class ESTimelineService @Inject() (
    clientService: SearchClientService,
    dateUtils: DateUtils,
    utils: ESRequestUtils
) extends TimelineService {

  private val timelineAggName = "timeline"

  /** @inheritdoc */
  override def createTimeline(facets: Facets, levelOfDetail: LoD.Value)(index: String): Aggregation = {
    val lodToFormat = Map(
      LoD.overview -> dateUtils.yearPattern,
      LoD.decade -> dateUtils.yearPattern,
      LoD.year -> dateUtils.yearMonthPattern,
      LoD.month -> dateUtils.yearMonthDayPattern
    )
    val parser = (r: SearchResponse) => parseTimeline(r)

    createDateTimeline(facets, utils.docDateField, facets.fromDate, facets.toDate, levelOfDetail, parser, lodToFormat, index)
  }

  /** @inheritdoc */
  override def createTimeExpressionTimeline(facets: Facets, levelOfDetail: LoD.Value)(index: String): Aggregation = {
    // The first element in the format is used as bucket key format
    val lodToFormat = Map(
      LoD.overview -> s"${dateUtils.yearPattern} || ${dateUtils.yearMonthPattern} || ${dateUtils.yearMonthDayPattern}",
      LoD.decade -> s"${dateUtils.yearPattern} || ${dateUtils.yearMonthPattern} || ${dateUtils.yearMonthDayPattern}",
      LoD.year -> s"${dateUtils.yearMonthPattern} || ${dateUtils.yearPattern} || ${dateUtils.yearMonthDayPattern}",
      LoD.month -> s"${dateUtils.yearMonthDayPattern} || ${dateUtils.yearMonthPattern} || ${dateUtils.yearPattern}"
    )
    val parser = (r: SearchResponse) => parseTimeExpressionTimeline(r, levelOfDetail, facets.fromTimeExpression, facets.toTimeExpression)

    createDateTimeline(facets, utils.docTimeExpressionField, facets.fromTimeExpression, facets.toTimeExpression, levelOfDetail, parser, lodToFormat, index)
  }

  private def createDateTimeline(
    facets: Facets,
    dateField: String,
    fromDate: Option[LocalDateTime],
    toDate: Option[LocalDateTime],
    levelOfDetail: LoD.Value,
    parser: SearchResponse => Aggregation,
    lodToFormat: Map[LoD.Value, String],
    index: String
  ): Aggregation = {
    var requestBuilder = utils.createSearchRequest(facets, 0, index, clientService)
    val (format, level, minBound, maxBound) = getParameter(fromDate, toDate, levelOfDetail, lodToFormat)

    val histogramAgg = AggregationBuilders
      .dateHistogram(timelineAggName)
      .field(dateField)
      .interval(level)
      .format(format)
      .minDocCount(0)

    val boundedAgg = if (minBound.isDefined || maxBound.isDefined) histogramAgg.extendedBounds(minBound.get, maxBound.get) else histogramAgg
    requestBuilder = requestBuilder.addAggregation(boundedAgg)

    val response = utils.executeRequest(requestBuilder, cache = false)
    val result = parser(response)

    levelOfDetail match {
      // Post process result if the overview is requested
      case LoD.overview =>
        val collectionFirstDate = minDate(dateField, index)
        val collectionLastDate = maxDate(dateField, index)

        groupToOverview(result.buckets, collectionFirstDate, collectionLastDate)
      case _ => result
    }
  }

  private def getParameter(
    fromDate: Option[LocalDateTime],
    toDate: Option[LocalDateTime],
    levelOfDetail: LoD.Value,
    lodToFormat: Map[LoD.Value, String]
  ): (String, DateHistogramInterval, Option[String], Option[String]) = {
    levelOfDetail match {
      case LoD.overview =>
        assert(fromDate.isEmpty)
        assert(toDate.isEmpty)
        (lodToFormat(levelOfDetail), DateHistogramInterval.YEAR, None, None)
      case LoD.decade =>
        val from = fromDate.map(_.toString(dateUtils.yearFormat))
        val to = toDate.map(_.toString(dateUtils.yearFormat))
        (lodToFormat(levelOfDetail), DateHistogramInterval.YEAR, from, to)
      case LoD.year =>
        val from = fromDate.map(_.toString(dateUtils.yearMonthFormat))
        val to = toDate.map(_.toString(dateUtils.yearMonthFormat))
        (lodToFormat(levelOfDetail), DateHistogramInterval.MONTH, from, to)
      case LoD.month =>
        val from = fromDate.map(_.toString(dateUtils.yearMonthDayFormat))
        val to = toDate.map(_.toString(dateUtils.yearMonthDayFormat))
        (lodToFormat(levelOfDetail), DateHistogramInterval.DAY, from, to)
      case _ => throw new IllegalArgumentException("Unknown level of detail.")
    }
  }

  private def minDate(field: String, index: String): LocalDateTime = {
    val aggName = "min_aggregation"
    val requestBuilder = utils.createSearchRequest(Facets.empty, 0, index, clientService)
    val agg = AggregationBuilders.min(aggName).field(field)
    requestBuilder.addAggregation(agg)

    val response = utils.executeRequest(requestBuilder)
    val res: Min = response.getAggregations.get(aggName)

    LocalDateTime.parse(res.getValueAsString, dateUtils.yearMonthDayFormat)
  }

  private def maxDate(field: String, index: String): LocalDateTime = {
    val aggName = "max_aggregation"
    val requestBuilder = utils.createSearchRequest(Facets.empty, 0, index, clientService)
    val agg = AggregationBuilders.max(aggName).field(field)
    requestBuilder.addAggregation(agg)

    val response = utils.executeRequest(requestBuilder)
    val res: Max = response.getAggregations.get(aggName)

    LocalDateTime.parse(res.getValueAsString, dateUtils.yearMonthDayFormat)
  }

  private def groupToOverview(originalBuckets: List[Bucket], minDate: LocalDateTime, maxDate: LocalDateTime): Aggregation = {
    def getDecade(date: LocalDateTime) = date.getYear - (date.getYear % 10)
    // Starting decade
    val firstDecade = getDecade(minDate)
    // Number of decades
    val numDecades = (getDecade(maxDate) - firstDecade) / 10

    // Create map from decade start to buckets
    val decadeToCount = originalBuckets.collect {
      case (b: MetaDataBucket) =>
        val decade = getDecade(LocalDateTime.parse(b.key, dateUtils.yearFormat))
        decade -> b.occurrence
    }.groupBy(_._1).mapValues(_.map(_._2))

    val buckets = (0 to numDecades).map { decade =>
      val startDecade = firstDecade + 10 * decade
      val endDecade = firstDecade + 9 + 10 * decade
      val key = s"$startDecade-$endDecade"
      MetaDataBucket(key, decadeToCount.getOrElse(startDecade, Nil).sum)
    }.toList

    Aggregation(timelineAggName, buckets)
  }

  private def parseTimeline(response: SearchResponse): Aggregation = {
    val agg = response.getAggregations.get(timelineAggName).asInstanceOf[Histogram]
    val buckets = agg.getBuckets.map(b => MetaDataBucket(b.getKeyAsString, b.getDocCount)).toList
    Aggregation(timelineAggName, buckets)
  }

  private def parseTimeExpressionTimeline(
    response: SearchResponse,
    lod: LoD.Value,
    from: Option[LocalDateTime],
    to: Option[LocalDateTime]
  ): Aggregation = {
    val agg = response.getAggregations.get(timelineAggName).asInstanceOf[Histogram]
    val buckets = agg.getBuckets.collect {
      // Filter date buckets, which are out of the given range.
      case b if lod != LoD.overview && isBetweenInclusive(from.get, to.get, b.getKeyAsString) =>
        MetaDataBucket(b.getKeyAsString, b.getDocCount)
      // Take everything from ES for the overview
      case b if lod == LoD.overview =>
        MetaDataBucket(b.getKeyAsString, b.getDocCount)
    }.toList
    Aggregation(timelineAggName, buckets)
  }

  private def isBetweenInclusive(from: LocalDateTime, to: LocalDateTime, target: String): Boolean = {
    val targetDate = LocalDateTime.parse(target)
    !targetDate.isBefore(from) && !targetDate.isAfter(to)
  }
}

