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

import models.{ Entity, Facets, NodeBucket }
import models.Entity.entityFormat
import models.services.{ AggregateService, EntityService }
import play.api.libs.json.Json.toJson
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.{ Action, Controller }
import util.SessionUtils.currentDataset
import util.DateUtils

/**
 * Provides entity related actions.
 *
 * @param entityService the service for entity backend operations.
 * @param aggregateService the aggregation service.
 * @param dateUtils common helper for date and time operations.
 */
class EntityController @Inject() (
    entityService: EntityService,
    aggregateService: AggregateService,
    dateUtils: DateUtils
) extends Controller {

  private val defaultFetchSize = 50

  /** Returns a ordered list of distinct entity types in the underlying collection. */
  def getEntityTypes = Action { implicit request =>
    val types = entityService.getTypes()(currentDataset).map { case (t, id) => Json.obj("name" -> t, "id" -> id) }
    Ok(toJson(types)).as("application/json")
  }

  /** Returns all blacklisted entities for the underlying collection. */
  def getBlacklistedEntities = Action { implicit request =>
    val entities = entityService.getBlacklisted()(currentDataset)
    Ok(toJson(entities)).as("application/json")
  }

  /** Returns all merged entities for the underlying collection. */
  def getMergedEntities = Action { implicit request =>
    val entities = entityService.getMerged()(currentDataset).map {
      case (focalNode, duplicates) =>
        Json.obj("id" -> focalNode.id, "origin" -> focalNode, "duplicates" -> duplicates)
    }
    Ok(toJson(entities)).as("application/json")
  }

  /**
   * Returns all entity occurrences for the given document including their position in the document.
   *
   * @param docId the document id.
   */
  def getEntitiesByDoc(docId: Long) = Action { implicit request =>
    val typesToId = entityService.getTypes()(currentDataset)
    val entityToOccurrences = entityService.getEntityFragments(docId)(currentDataset).groupBy(_._1)
    val res = entityToOccurrences.flatMap {
      case (Entity(id, name, t, _), occ) =>
        occ.map {
          case (_, fragment) =>
            Json.obj("id" -> id, "name" -> name, "type" -> t, "typeId" -> typesToId(t), "start" -> fragment.start, "end" -> fragment.end)
        }
    }
    Ok(toJson(res)).as("application/json")
  }

  /**
   * Removes the blacklisted mark from the entities associated with the given ids.
   *
   * @param ids the entity ids to remove the blacklist mark from.
   */
  def undoBlacklistingByIds(ids: List[Long]) = Action { implicit request =>
    entityService.undoBlacklist(ids)(currentDataset)
    Ok("success").as("Text")
  }

  /**
   * Withdraws [[models.services.EntityService#merge]] for the given entity id.
   *
   * @param focalIds the central entity ids.
   */
  def undoMergeByIds(focalIds: List[Long]) = Action { implicit request =>
    entityService.undoMerge(focalIds)(currentDataset)
    Ok("success").as("Text")
  }

  // scalastyle:off
  /**
   * Returns frequent entities occurring in the documents matching the given query.
   *
   * @param fullText match documents that contain the given expression in the document body.
   * @param generic a map linking from document metadata keys to a list of instances for this metadata.
   * @param entities a list of entity ids that should occur in the document.
   * @param timeRange a string representing a time range for the document creation date.
   * @param timeExprRange a string representing a time range for the document time expression.
   * @param size the number of entities to fetch.
   * @param entityType the entity type to fetch.
   * @param filter a list of entity ids to filter the result.
   * @return list of matching entity id's and their overall frequency as well as document count for the applied filters
   */
  def getEntitiesByType(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeExprRange: String,
    size: Int,
    entityType: String,
    filter: List[Long]
  ) = Action { implicit request =>
    val (from, to) = dateUtils.parseTimeRange(timeRange)
    val (timeExprFrom, timeExprTo) = dateUtils.parseTimeRange(timeExprRange)
    val facets = Facets(fullText, generic, entities, from, to, timeExprFrom, timeExprTo)
    var newSize = size

    val blacklistedIds = entityService.getBlacklisted()(currentDataset).map(_.id)
    // Filter list without blacklisted entities
    val validFilter = if (filter.nonEmpty) {
      newSize = filter.length
      filter.filterNot(blacklistedIds.contains(_))
    } else filter

    val entitiesRes = aggregateService.aggregateEntitiesByType(facets, entityType, newSize, validFilter, blacklistedIds)(currentDataset).buckets.collect { case NodeBucket(id, count) => (id, count) }

    if (entitiesRes.nonEmpty) {
      val ids = entitiesRes.map(_._1).take(defaultFetchSize)
      val sqlResult = entityService.getByIds(ids)(currentDataset).map(e => e.id -> e)
      // TODO: ordering commented out while no zero buckets available
      if (filter.nonEmpty) {
        // if (false) {
        val res = validFilter
          .zip(validFilter.map(sqlResult.toMap))
          .map(x => Json.obj(
            "id" -> x._2.id,
            "name" -> x._2.name,
            "type" -> x._2.entityType,
            "freq" -> x._2.freq,
            "docCount" -> entitiesRes.find(_._1 == x._2.id).get._2.asInstanceOf[Number].longValue
          ))
        Ok(toJson(res)).as("application/json")
      } else {
        val res = sqlResult.map(x => Json.obj(
          "id" -> x._2.id,
          "name" -> x._2.name,
          "type" -> x._2.entityType,
          "freq" -> x._2.freq,
          "docCount" -> entitiesRes.find(_._1 == x._2.id).get._2.asInstanceOf[Number].longValue
        ))
        Ok(toJson(res.sortBy(-_.value("docCount").as[Long]))).as("application/json")
      }
    } else {
      Ok(toJson(List[JsObject]())).as("application/json")
    }
  }
  // scalastyle:on
}
