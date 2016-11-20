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

import models._
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.{ Action, AnyContent, Controller, Request }
import util.SessionUtils.currentDataset
import util.TimeRangeParser
import EntityType._

// scalastyle:off
import util.TupleWriters._
// scalastyle:off

/**
 * This class encapsulates all functionality for the
 * network graph.
 */
class NetworkController @Inject() (entityService: EntityService, networkService: NetworkService) extends Controller {

  def getNeighborCounts(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeRangeX: String,
    nodeId: Long
  ) = Action { implicit request =>

    val times = TimeRangeParser.parseTimeRange(timeRange)
    val timesX = TimeRangeParser.parseTimeRange(timeRangeX)
    val facets = Facets(fullText, generic, entities, times.from, times.to, timesX.from, timesX.to)

    val agg = networkService.getNeighborCountsPerType(facets, nodeId)(currentDataset)

    val counts = agg.buckets.collect {
      case MetaDataBucket(t, c) =>
        Json.obj("type" -> t, "count" -> c)
    }
    Ok(Json.toJson(counts)).as("application/json")
  }

  def getEdgeKeywords(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeRangeX: String,
    first: Long,
    second: Long,
    numberOfTerms: Int
  ) = Action { implicit request =>

    val times = TimeRangeParser.parseTimeRange(timeRange)
    val timesX = TimeRangeParser.parseTimeRange(timeRangeX)
    val facets = Facets(fullText, generic, entities, times.from, times.to, timesX.from, timesX.to)

    val agg = networkService.getEdgeKeywords(facets, first, second, numberOfTerms)(currentDataset)

    val terms = agg.buckets.collect {
      case MetaDataBucket(term, score) =>
        Json.obj("term" -> term, "score" -> score)
    }
    Ok(Json.toJson(terms)).as("application/json")
  }

  def induceSubgraph(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeRangeX: String,
    nodeFraction: Map[String, String]
  ) = Action { implicit request =>
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val timesX = TimeRangeParser.parseTimeRange(timeRangeX)
    val facets = Facets(fullText, generic, entities, times.from, times.to, timesX.from, timesX.to)
    val sizes = nodeFraction.mapValues(_.toInt)

    val blacklistedIds = entityService.getBlacklisted()(currentDataset).map(_.id)
    val (nodes, relations) = networkService.createNetwork(facets, sizes, blacklistedIds)(currentDataset)

    if (nodes.isEmpty) {
      Ok(Json.obj("entities" -> List[JsObject](), "relations" -> List[JsObject]())).as("application/json")
    } else {
      val graphEntities = nodesToJson(nodes)
      // Ignore relations that connect blacklisted nodes
      val graphRelations = relations.filterNot { case (from, to, _) => blacklistedIds.contains(from) && blacklistedIds.contains(to) }

      val types = Json.obj(
        Person.toString -> Person.id,
        Organization.toString -> Organization.id,
        Location.toString -> Location.id,
        Misc.toString -> Misc.id
      )
      Ok(Json.obj("entities" -> graphEntities, "relations" -> graphRelations, "types" -> types)).as("application/json")
    }
  }

  def addNodes(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeRangeX: String,
    currentNetwork: List[Long],
    nodes: List[Long]
  ) = Action { implicit request =>
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val timesX = TimeRangeParser.parseTimeRange(timeRangeX)
    val facets = Facets(fullText, generic, entities, times.from, times.to, timesX.from, timesX.to)

    val (buckets, relations) = networkService.induceNetwork(facets, currentNetwork, nodes)(currentDataset)

    Ok(Json.obj("entities" -> nodesToJson(buckets), "relations" -> relations)).as("application/json")
  }

  // TODO: Use json writer and reader to minimize parameter in a case class Facets
  def getNeighbors(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeRangeX: String,
    currentNetwork: List[Long],
    focalNode: Long
  ) = Action { implicit request =>

    // TODO Duplicated code to parse facets
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val timesX = TimeRangeParser.parseTimeRange(timeRangeX)
    val facets = Facets(fullText, generic, entities, times.from, times.to, timesX.from, timesX.to)

    // TODO: we don't need to add the blacklist as exclude when we use getById.contains
    val blacklistedIds = entityService.getBlacklisted()(currentDataset).map(_.id)
    val nodes = networkService.getNeighbors(facets, focalNode, 200, blacklistedIds ++ currentNetwork)(currentDataset)

    val neighbors = nodesToJson(nodes)
    Ok(Json.toJson(neighbors)).as("application/json")
  }

  def nodesToJson(nodes: List[NodeBucket])(implicit request: Request[AnyContent]): List[JsObject] = {
    val ids = nodes.map(_.id)
    val nodeIdToEntity = entityService.getByIds(ids)(currentDataset).map(e => e.id -> e).toMap

    nodes.collect {
      // Only add node if it is not blacklisted
      case NodeBucket(id, count) if nodeIdToEntity.contains(id) =>
        val node = nodeIdToEntity(id)
        Json.obj(
          "id" -> id,
          "label" -> node.name,
          "count" -> count,
          "type" -> node.entityType.toString,
          "group" -> node.entityType.id
        )
    }
  }

  def blacklistEntitiesById(ids: List[Long]) = Action { implicit request =>
    Ok(Json.obj("result" -> entityService.blacklist(ids)(currentDataset))).as("application/json")
  }

  /**
   * merge all entities into one entity represented by the focalId
   *
   * @param focalId the entity to merge into
   * @param duplicates     the ids of the entities which are duplicates of
   *                the focal entity
   * @return if the merging succeeded
   */
  def mergeEntitiesById(focalId: Long, duplicates: List[Long]) = Action { implicit request =>
    entityService.merge(focalId, duplicates)(currentDataset)
    Ok("success").as("Text")
  }

  /**
   * change the entity name by a new name of the given Entity
   *
   * @param id      the id of the entity to change
   * @param newName the new name of the entity
   * @return if the change succeeded
   */
  def changeEntityNameById(id: Long, newName: String) = Action { implicit request =>
    Ok(Json.obj("result" -> entityService.changeName(id, newName)(currentDataset))).as("application/json")
  }

  /**
   * change the entity type by a new type
   *
   * @param id      the id of the entity to change
   * @param newType the new type of the entity
   * @return if the change succeeded
   */
  def changeEntityTypeById(id: Long, newType: String) = Action { implicit request =>
    Ok(Json.obj("result" -> entityService.changeType(id, newType)(currentDataset))).as("application/json")
  }
}
