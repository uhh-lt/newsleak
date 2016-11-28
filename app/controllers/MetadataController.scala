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

import models.services.{ AggregateService, DocumentService }
import models.{ Facets, MetaDataBucket }
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller, Results }
import util.SessionUtils.currentDataset
import util.{ NewsleakConfigReader, DateUtils }

/**
 * Provides metadata related actions.
 *
 * @param documentService the service for document backend operations.
 * @param aggregateService the aggregation service.
 * @param dateUtils common helper for date and time operations.
 */
class MetadataController @Inject() (
    documentService: DocumentService,
    aggregateService: AggregateService,
    dateUtils: DateUtils
) extends Controller {

  private lazy val defaultExcludeTypes = NewsleakConfigReader.excludedMetadataTypes
  // TODO Duplicate from other controller
  private val defaultFetchSize = 50

  /** Returns all unique metadata keys for the underlying collection. */
  def getMetadataTypes = Action { implicit request =>
    val keys = documentService.getMetadataKeys()(currentDataset).filter(!defaultExcludeTypes(currentDataset).contains(_))
    Results.Ok(Json.toJson(keys)).as("application/json")
  }

  /**
   * Gets document counts for all metadata types corresponding to their keys matching the given query.
   *
   * @param fullText match documents that contain the given expression in the document body.
   * @param generic a map linking from document metadata keys to a list of instances for this metadata.
   * @param entities a list of entity ids that should occur in the document.
   * @param timeRange a string representing a time range for the document creation date.
   * @param timeExprRange a string representing a time range for the document time expression.
   * @return list of matching metadata keys and document counts.
   */
  def getMetadata(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeExprRange: String
  ) = Action { implicit request =>
    val (from, to) = dateUtils.parseTimeRange(timeRange)
    val (timeExprFrom, timeExprTo) = dateUtils.parseTimeRange(timeExprRange)
    val facets = Facets(fullText, generic, entities, from, to, timeExprFrom, timeExprTo)
    val res = aggregateService.aggregateAll(facets, defaultFetchSize, defaultExcludeTypes(currentDataset))(currentDataset)
      .map(agg => Json.obj(agg.key -> agg.buckets.map {
        case MetaDataBucket(key, count) => Json.obj("key" -> key, "count" -> count)
        case _ => Json.obj()
      }))
    Results.Ok(Json.toJson(res)).as("application/json")
  }

  /**
   * Returns document counts for a metadata type matching the given key and search query.
   *
   * @param fullText match documents that contain the given expression in the document body.
   * @param key the metadata key to aggregate.
   * @param generic a map linking from document metadata keys to a list of instances for this metadata.
   * @param entities a list of entity ids that should occur in the document.
   * @param timeRange a string representing a time range for the document creation date.
   * @param timeExprRange a string representing a time range for the document time expression.
   * @return list of matching metadata keys and document counts.
   */
  def getSpecificMetadata(
    fullText: List[String],
    key: String,
    generic: Map[String, List[String]],
    entities: List[Long],
    instances: List[String],
    timeRange: String,
    timeExprRange: String
  ) = Action { implicit request =>
    val (from, to) = dateUtils.parseTimeRange(timeRange)
    val (timeExprFrom, timeExprTo) = dateUtils.parseTimeRange(timeExprRange)
    val facets = Facets(fullText, generic, entities, from, to, timeExprFrom, timeExprTo)

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
