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

import model.faceted.search.{ Facets, FacetedSearch, MetaDataBucket, LoD }
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller, Results }
import util.TimeRangeParser

// scalastyle:off
import scalikejdbc._
// scalastyle:on

/**
 * Created by flo on 6/20/2016.
 */
class HistogramController @Inject extends Controller {
  implicit val session = AutoSession

  private val defaultFetchSize = 50

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
    lod: String
  ) = Action {
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val facets = Facets(fullText, generic, entities, times.from, times.to)
    val res = FacetedSearch.histogram(facets, LoD.withName(lod)).buckets.map {
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
