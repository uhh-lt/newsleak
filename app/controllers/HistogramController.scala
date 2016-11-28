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

import models.services.TimelineService
import models.{ Facets, LoD, MetaDataBucket }
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller, Results }
import util.SessionUtils.currentDataset
import util.DateUtils

// TODO: rename Histogram to Timeline and refactor code duplication
/**
 * Provides timeline related actions.
 *
 * @param timelineService the timeline backend service.
 * @param dateUtils common helper for date and time operations.
 */
class HistogramController @Inject() (timelineService: TimelineService, dateUtils: DateUtils) extends Controller {

  /**
   * Returns entity document counts based on the document creation date matching the search query.
   *
   * @param fullText match documents that contain the given expression in the document body.
   * @param generic a map linking from document metadata keys to a list of instances for this metadata.
   * @param entities a list of entity ids that should occur in the document.
   * @param timeRange a string representing a time range for the document creation date.
   * @param timeExprRange a string representing a time range for the document time expression.
   * @param lod the level of detail value. One of [[models.LoD]].
   * @return a list of entity id's and their occurrence in the documents matching the search query.
   */
  def getTimeline(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeExprRange: String,
    lod: String
  ) = Action { implicit request =>
    val (from, to) = dateUtils.parseTimeRange(timeRange)
    val (timeExprFrom, timeExprTo) = dateUtils.parseTimeRange(timeExprRange)
    val facets = Facets(fullText, generic, entities, from, to, timeExprFrom, timeExprTo)

    val res = timelineService.createTimeline(facets, LoD.withName(lod))(currentDataset).buckets.map {
      case MetaDataBucket(key, count) => Json.obj("range" -> key, "count" -> count)
      case _ => Json.obj("" -> 0)
    }
    Results.Ok(Json.toJson(Json.obj("histogram" -> Json.toJson(res)))).as("application/json")
  }

  /**
   * Returns entity document counts based on the time expression occurring in the documents matching the search query.
   *
   * @param fullText match documents that contain the given expression in the document body.
   * @param generic a map linking from document metadata keys to a list of instances for this metadata.
   * @param entities a list of entity ids that should occur in the document.
   * @param timeRange a string representing a time range for the document creation date.
   * @param timeExprRange a string representing a time range for the document time expression.
   * @param lod the level of detail value. One of [[models.LoD]].
   * @return a list of entity id's and their occurrence in the documents matching the search query.
   */
  def getTimeExprTimeline(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeExprRange: String,
    lod: String
  ) = Action { implicit request =>
    val (from, to) = dateUtils.parseTimeRange(timeRange)
    val (timeExprFrom, timeExprTo) = dateUtils.parseTimeRange(timeExprRange)
    val facets = Facets(fullText, generic, entities, from, to, timeExprFrom, timeExprTo)

    val res = timelineService.createTimeExpressionTimeline(facets, LoD.withName(lod))(currentDataset).buckets.map {
      case MetaDataBucket(key, count) => Json.obj("range" -> key, "count" -> count)
      case _ => Json.obj("" -> 0)
    }
    Results.Ok(Json.toJson(Json.obj("histogram" -> Json.toJson(res)))).as("application/json")
  }

  /** Returns the available instances for the level of detail type [[models.LoD]]. */
  def getTimelineLOD = Action {
    Results.Ok(Json.toJson(LoD.values.toList.map(_.toString))).as("application/json")
  }
}
