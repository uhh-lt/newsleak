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

package controllers

import javax.inject.Inject

import model.faceted.search.{ Facets, MetaDataBucket }
import models.{ AggregateService, DocumentService }
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller, Results }
import util.SessionUtils.currentDataset
import util.TimeRangeParser
import utils.NewsleakConfigReader

class MetadataController @Inject() (documentService: DocumentService, aggregateService: AggregateService) extends Controller {

  private lazy val defaultExcludeTypes = NewsleakConfigReader.excludedMetadataTypes

  // TODO Duplicate from other controller
  private val defaultFetchSize = 50

  /**
   * Get all metadata types
   * @return list of metadata types
   */
  def getMetadataTypes = Action { implicit request =>
    val keys = documentService.getMetadataKeys()(currentDataset).filter(!defaultExcludeTypes(currentDataset).contains(_))
    Results.Ok(Json.toJson(keys)).as("application/json")
  }

  /**
   * Gets document counts for all metadata types corresponding to their keys
   * @param fullText Full text search term
   * @param generic mapping of metadata key and a list of corresponding tags
   * @param entities list of entity ids to filter
   * @param timeRange string of a time range readable for [[TimeRangeParser]]
   * @return list of matching metadata keys and document count
   */
  def getMetadata(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeRangeX: String
  ) = Action { implicit request =>
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val timesX = TimeRangeParser.parseTimeRange(timeRangeX)
    val facets = Facets(fullText, generic, entities, times.from, times.to, timesX.from, timesX.to)
    val res = aggregateService.aggregateAll(facets, defaultFetchSize, defaultExcludeTypes(currentDataset))(currentDataset)
      .map(agg => Json.obj(agg.key -> agg.buckets.map {
        case MetaDataBucket(key, count) => Json.obj("key" -> key, "count" -> count)
        case _ => Json.obj()
      }))
    Results.Ok(Json.toJson(res)).as("application/json")
  }

  /**
   * Gets document counts for one metadata types corresponding to their keys for given list of instances
   * @param fullText Full text search term
   * @param key metadata type key to aggregate on
   * @param generic mapping of metadata key and a list of corresponding tags
   * @param entities list of entity ids to filter
   * @param instances list of metadata instaces to buckets for
   * @param timeRange string of a time range readable for [[TimeRangeParser]]
   * @return list of matching metadata keys and document count
   */
  def getSpecificMetadata(
    fullText: List[String],
    key: String,
    generic: Map[String, List[String]],
    entities: List[Long],
    instances: List[String],
    timeRange: String,
    timeRangeX: String
  ) = Action { implicit request =>
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val timesX = TimeRangeParser.parseTimeRange(timeRangeX)
    val facets = Facets(fullText, generic, entities, times.from, times.to, timesX.from, timesX.to)

    val agg = aggregateService.aggregate(facets, key, defaultFetchSize, instances, Nil)(currentDataset)

    if (instances.isEmpty) {
      val res = Json.obj(key -> agg.buckets.map {
        case MetaDataBucket(metaKey, count) => Json.obj("key" -> metaKey, "count" -> count)
        case _ => Json.obj()
      })
      Results.Ok(Json.toJson(res)).as("application/json")
    } else {
      val res = instances.zip(instances.map(agg.buckets.map {
        case MetaDataBucket(metaKey, count) => metaKey -> count
        case _ => "" -> 0.0
      }.toMap)).map(x => Json.obj("key" -> x._1, "count" -> x._2.asInstanceOf[Number].longValue()))
      Results.Ok(Json.obj(key -> res)).as("application/json")
    }
  }
}
