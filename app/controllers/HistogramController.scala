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

import model.faceted.search.{ Facets, LoD, MetaDataBucket }
import models.TimelineService
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller, Results }
import util.SessionUtils.currentDataset
import util.TimeRangeParser

// TODO: rename Histogram to Timeline and refactor code duplication
// TODO: rename histogramX to TimeExpression ...
class HistogramController @Inject() (timelineService: TimelineService) extends Controller {

  /**
   * Gets document counts for entities corresponding to their id's matching the query
   * @param fullText Full text search term
   * @param generic   mapping of metadata key and a list of corresponding tags
   * @param entities list of entity ids to filter
   * @param timeRange string of a time range readable for [[TimeRangeParser]]
   * @param lod Level of detail, value of[[LoD]]
   * @return list of matching entity id's and their overall frequency as well as document count for the applied filters
   */
  def getHistogram(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeRangeX: String,
    lod: String
  ) = Action { implicit request =>
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val timesX = TimeRangeParser.parseTimeRange(timeRangeX)
    val facets = Facets(fullText, generic, entities, times.from, times.to, timesX.from, timesX.to)
    val res = timelineService.createTimeline(facets, LoD.withName(lod))(currentDataset).buckets.map {
      case MetaDataBucket(key, count) => Json.obj("range" -> key, "count" -> count)
      case _ => Json.obj("" -> 0)
    }
    Results.Ok(Json.toJson(Json.obj("histogram" -> Json.toJson(res)))).as("application/json")
  }

  /**
   * Gets document counts for entities corresponding to their id's matching the query
   * @param fullText Full text search term
   * @param generic   mapping of metadata key and a list of corresponding tags
   * @param entities list of entity ids to filter
   * @param timeRange string of a time range readable for [[TimeRangeParser]]
   * @param lod Level of detail, value of[[LoD]]
   * @return list of matching entity id's and their overall frequency as well as document count for the applied filters
   */
  def getXHistogram(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeRangeX: String,
    lod: String
  ) = Action { implicit request =>
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val timesX = TimeRangeParser.parseTimeRange(timeRangeX)
    val facets = Facets(fullText, generic, entities, times.from, times.to, timesX.from, timesX.to)
    val res = timelineService.createTimeExpressionTimeline(facets, LoD.withName(lod))(currentDataset).buckets.map {
      case MetaDataBucket(key, count) => Json.obj("range" -> key, "count" -> count)
      case _ => Json.obj("" -> 0)
    }
    Results.Ok(Json.toJson(Json.obj("histogram" -> Json.toJson(res)))).as("application/json")
  }

  /**
   *
   * @return identifiers for the levels of detail in backend API
   */
  def getHistogramLod = Action {
    Results.Ok(Json.toJson(LoD.values.toList.map(_.toString))).as("application/json")
  }
}
