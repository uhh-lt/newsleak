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

import model.Entity
import model.faceted.search.{ FacetedSearch, Facets, MetaDataBucket, NodeBucket }
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.{ Action, AnyContent, Controller, Request }
import util.SessionUtils.currentDataset
import util.TimeRangeParser

// scalastyle:off
import model.EntityType._
import util.TupleWriters._
// scalastyle:off

/**
 * This class encapsulates all functionality for the
 * network graph.
 */
class NetworkController @Inject extends Controller {

  // TODO: These methods should actually part of the entity controller
  def getIdsByName(name: String) = Action { implicit request =>
    Ok(Json.obj("ids" -> Entity.fromDBName(currentDataset).getByName(name).map(_.id))).as("application/json")
  }

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

    val agg = FacetedSearch
      .fromIndexName(currentDataset)
      .getNeighborCounts(facets, nodeId)

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

    val agg = FacetedSearch
      .fromIndexName(currentDataset)
      // Only consider documents where the two entities occur
      .aggregateKeywords(facets.withEntities(List(first, second)), numberOfTerms, Nil)

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
    val sizes = nodeFraction.map { case (t, s) => withName(t) -> s.toInt }

    val blacklistedIds = Entity.fromDBName(currentDataset).getBlacklisted().map(_.id)
    val (buckets, relations) = FacetedSearch
      .fromIndexName(currentDataset)
      .induceSubgraph(facets, sizes, blacklistedIds)
    // TODO Let induceSubgraph return NodeBuckets
    val nodes = buckets.collect { case a @ NodeBucket(_, _) => a }

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

    val (buckets, relations) = FacetedSearch
      .fromIndexName(currentDataset)
      .addNodes(facets, currentNetwork, nodes)

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
    val blacklistedIds = Entity.fromDBName(currentDataset).getBlacklisted().map(_.id)
    val agg = FacetedSearch
      .fromIndexName(currentDataset)
      .aggregateEntities(facets.withEntities(List(focalNode)), 200, List(), blacklistedIds ++ currentNetwork, 1)

    val nodes = agg.buckets.collect { case a @ NodeBucket(_, _) => a }
    val neighbors = nodesToJson(nodes)
    Ok(Json.toJson(neighbors)).as("application/json")
  }

  def nodesToJson(nodes: List[NodeBucket])(implicit request: Request[AnyContent]): List[JsObject] = {
    val ids = nodes.map(_.id)
    val nodeIdToEntity = Entity.fromDBName(currentDataset).getByIds(ids).map(e => e.id -> e).toMap

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
    val entityAPI = Entity.fromDBName(currentDataset)
    val response = ids.map(entityAPI.delete).forall(identity)
    Ok(Json.obj("result" -> response)).as("application/json")
  }

  /**
   * merge all entities into one entity represented by the focalId
   *
   * @param focalId the entity to merge into
   * @param duplicates     the ids of the entities which are duplicates of
   *                the focal entity
   * @return if the merging succeeded
   */
  def mergeEntitiesById(focalId: Int, duplicates: List[Long]) = Action { implicit request =>
    Entity.fromDBName(currentDataset).merge(focalId, duplicates)
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
    Ok(Json.obj("result" -> Entity.fromDBName(currentDataset).changeName(id, newName))).as("application/json")
  }

  /**
   * change the entity type by a new type
   *
   * @param id      the id of the entity to change
   * @param newType the new type of the entity
   * @return if the change succeeded
   */
  def changeEntityTypeById(id: Long, newType: String) = Action { implicit request =>
    Ok(Json.obj("result" -> Entity.fromDBName(currentDataset).changeType(id, withName(newType)))).as("application/json")
  }
}
