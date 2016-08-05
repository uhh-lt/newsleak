/*
 * Copyright 2015 Technische Universitaet Darmstadt
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
