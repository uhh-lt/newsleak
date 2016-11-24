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
import models.services.{ AggregateService, EntityService }
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.{ Action, Controller }
import util.SessionUtils.currentDataset
import util.TimeRangeParser

class EntityController @Inject() (entityService: EntityService, aggregateService: AggregateService) extends Controller {

  private val defaultFetchSize = 50

  def getEntityTypes = Action { implicit request =>
    Ok(Json.toJson(entityService.getTypes()(currentDataset))).as("application/json")
  }

  def getBlacklistedEntities = Action { implicit request =>
    val entities = entityService.getBlacklisted()(currentDataset)
      // TODO use Entity json mapper
      .map(x => Json.obj("id" -> x.id, "name" -> x.name, "freq" -> x.occurrence, "type" -> x.entityType))
    Ok(Json.toJson(entities)).as("application/json")
  }

  def getMergedEntities = Action { implicit request =>
    val entities = entityService.getMerged()(currentDataset).map {
      case (focalNode, duplicates) =>
        val focalFormat = Json.obj("id" -> focalNode.id, "name" -> focalNode.name, "freq" -> focalNode.occurrence, "type" -> focalNode.entityType)
        val duplicateFormat = duplicates.map(d => Json.obj("id" -> d.id, "name" -> d.name, "freq" -> d.occurrence, "type" -> d.entityType))

        Json.obj("id" -> focalNode.id, "origin" -> focalFormat, "duplicates" -> Json.toJson(duplicateFormat))
    }
    Ok(Json.toJson(entities)).as("application/json")
  }

  // TODO Json writer for model types ...
  def getEntitiesByDoc(docId: Long) = Action { implicit request =>
    val entityToOccurrences = entityService.getEntityFragments(docId)(currentDataset).groupBy(_._1)
    val res = entityToOccurrences.flatMap {
      case (Entity(id, name, t, _), occ) =>
        occ.map { case (_, fragment) => Json.obj("id" -> id, "name" -> name, "type" -> t, "start" -> fragment.start, "end" -> fragment.end) }
    }
    Ok(Json.toJson(res)).as("application/json")
  }

  def undoBlacklistingByIds(ids: List[Long]) = Action { implicit request =>
    entityService.undoBlacklist(ids)(currentDataset)
    Ok("success").as("Text")
  }

  def undoMergeByIds(focalIds: List[Long]) = Action { implicit request =>
    entityService.undoMerge(focalIds)(currentDataset)
    Ok("success").as("Text")
  }

  // scalastyle:off
  /**
   * Gets document counts for entities corresponding to their id's matching the query
   * @param fullText Full text search term
   * @param generic   mapping of metadata key and a list of corresponding tags
   * @param entities list of entity ids to filter
   * @param timeRange string of a time range readable for [[TimeRangeParser]]
   * @param size amount of entities to fetch
   * @param filter provide a list of entities you want to aggregate
   * @return list of matching entity id's and their overall frequency as well as document count for the applied filters
   */
  def getEntities(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeRangeX: String,
    size: Int,
    entityType: String,
    filter: List[Long]
  ) = Action { implicit request =>
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val timesX = TimeRangeParser.parseTimeRange(timeRangeX)
    val facets = Facets(fullText, generic, entities, times.from, times.to, timesX.from, timesX.to)
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
            "freq" -> x._2.occurrence,
            "docCount" -> entitiesRes.find(_._1 == x._2.id).get._2.asInstanceOf[Number].longValue
          ))
        Ok(Json.toJson(res)).as("application/json")
      } else {
        val res = sqlResult.map(x => Json.obj(
          "id" -> x._2.id,
          "name" -> x._2.name,
          "type" -> x._2.entityType,
          "freq" -> x._2.occurrence,
          "docCount" -> entitiesRes.find(_._1 == x._2.id).get._2.asInstanceOf[Number].longValue
        ))
        Ok(Json.toJson(res.sortBy(-_.value("docCount").as[Long]))).as("application/json")
      }
    } else {
      Ok(Json.toJson(List[JsObject]())).as("application/json")
    }
  }
  // scalastyle:on
}
