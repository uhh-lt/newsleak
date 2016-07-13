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

import model.faceted.search.{ FacetedSearch, Facets, MetaDataBucket }
import model.{ Document, Entity, EntityType }
import play.api.libs.json.{ JsArray, JsObject, Json, Writes }
import play.api.mvc.{ Action, Controller, Results }

/**
 * Created by f. zouhar on 26.05.16.
 */
class MetadataController @Inject extends Controller {
  // http://stackoverflow.com/questions/30921821/play-scala-json-writer-for-seq-of-tuple
  implicit def tuple2Writes[A, B](implicit a: Writes[A], b: Writes[B]): Writes[Tuple2[A, B]] = new Writes[Tuple2[A, B]] {
    def writes(tuple: Tuple2[A, B]) = JsArray(Seq(a.writes(tuple._1), b.writes(tuple._2)))
  }

  private val defaultExcludeTypes = List("Subject", "Header", "ReferenceId", "References", "Keywords", "Entities", "Created", "EventTimes")
  private val defaultFetchSize = 50

  /**
   * Get all metadata types
   * @return list of metadata types
   */
  def getMetadataTypes = Action {
    Results.Ok(Json.toJson(Document.getMetadataKeysAndTypes().map(x => x._1).filter(!defaultExcludeTypes.contains(_)))).as("application/json")
  }

  /**
   * Gets document counts for all metadata types corresponding to their keys
   * @param fullText Full text search term
   * @param generic mapping of metadata key and a list of corresponding tags
   * @param entities list of entity ids to filter
   * @return list of matching metadata keys and document count
   */
  def getMetadata(fullText: Option[String], generic: Map[String, List[String]], entities: List[Long]) = Action {
    val facets = Facets(fullText, generic, entities, None, None)
    val res = FacetedSearch.aggregateAll(facets, defaultFetchSize, defaultExcludeTypes)
      .map(agg => Json.obj(agg.key -> agg.buckets.map(x => x match {
        case MetaDataBucket(key, count) => Json.obj("key" -> key, "count" -> count)
        case _ => Json.obj()
      })))
    Results.Ok(Json.toJson(res)).as("application/json")
  }

  /**
   * Gets document counts for keywords
   * @param fullText Full text search term
   * @param generic mapping of metadata key and a list of corresponding tags
   * @param entities list of entity ids to filter
   * @return list of matching keywords and document count
   */
  def getKeywords(fullText: Option[String], generic: Map[String, List[String]], entities: List[Long]) = Action {
    val facets = Facets(fullText, generic, entities, None, None)
    val res = FacetedSearch.aggregateKeywords(facets, defaultFetchSize).buckets.map(x => x match {
      case MetaDataBucket(key, count) => Json.obj("key" -> key, "count" -> count)
      case _ => Json.obj()
    })

    Results.Ok(Json.toJson(res)).as("application/json")
  }
}
