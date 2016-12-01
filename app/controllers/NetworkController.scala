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

import models.{ Facets, Network, NodeBucket, Relationship }
import models.KeyTerm.keyTermFormat
import models.services.{ EntityService, NetworkService }
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.{ Action, AnyContent, Controller, Request }
import util.SessionUtils.currentDataset
import util.DateUtils

/**
 * Provides network related actions.
 *
 * @param entityService the service for entity backend operations.
 * @param networkService the service for network backend operations.
 * @param dateUtils common helper for date and time operations.
 */
class NetworkController @Inject() (
    entityService: EntityService,
    networkService: NetworkService,
    dateUtils: DateUtils
) extends Controller {

  private val numberOfNeighbors = 200

  /**
   * Accumulates the number of entities that fall in a certain entity type and co-occur with the given entity.
   *
   * @param fullText match documents that contain the given expression in the document body.
   * @param generic a map linking from document metadata keys to a list of instances for this metadata.
   * @param entities a list of entity ids that should occur in the document.
   * @param timeRange a string representing a time range for the document creation date.
   * @param timeExprRange a string representing a time range for the document time expression.
   * @param nodeId the entity id.
   * @return mapping from unique entity types to the number of neighbors of that type.
   */
  def getNeighborCounts(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeExprRange: String,
    nodeId: Long
  ) = Action { implicit request =>

    val (from, to) = dateUtils.parseTimeRange(timeRange)
    val (timeExprFrom, timeExprTo) = dateUtils.parseTimeRange(timeExprRange)
    val facets = Facets(fullText, generic, entities, from, to, timeExprFrom, timeExprTo)

    val res = networkService.getNeighborCountsPerType(facets, nodeId)(currentDataset)
    val counts = res.map { case (t, c) => Json.obj("type" -> t, "count" -> c) }
    Ok(Json.toJson(counts)).as("application/json")
  }

  /**
   * Returns important terms representing the relationship between both nodes based on the underlying document content.
   *
   * @param fullText match documents that contain the given expression in the document body.
   * @param generic a map linking from document metadata keys to a list of instances for this metadata.
   * @param entities a list of entity ids that should occur in the document.
   * @param timeRange a string representing a time range for the document creation date.
   * @param timeExprRange a string representing a time range for the document time expression.
   * @param first the first adjacent node of the edge.
   * @param second the second adjacent node of the edge.
   * @param numberOfTerms the number of keywords to fetch.
   * @return a list of [[models.KeyTerm]] representing important terms for the given relationship.
   */
  def getEdgeKeywords(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeExprRange: String,
    first: Long,
    second: Long,
    numberOfTerms: Int
  ) = Action { implicit request =>
    val (from, to) = dateUtils.parseTimeRange(timeRange)
    val (timeExprFrom, timeExprTo) = dateUtils.parseTimeRange(timeExprRange)
    val facets = Facets(fullText, generic, entities, from, to, timeExprFrom, timeExprTo)

    val terms = networkService.getEdgeKeywords(facets, first, second, numberOfTerms)(currentDataset)
    Ok(Json.toJson(terms)).as("application/json")
  }

  /**
   * Returns a co-occurrence network matching the given search query.
   *
   * @param fullText match documents that contain the given expression in the document body.
   * @param generic a map linking from document metadata keys to a list of instances for this metadata.
   * @param entities a list of entity ids that should occur in the document.
   * @param timeRange a string representing a time range for the document creation date.
   * @param timeExprRange a string representing a time range for the document time expression.
   * @param nodeFraction a map linking from entity types to the number of nodes to request for each type.
   * @return a network consisting of the nodes and relationships of the created co-occurrence network.
   */
  def induceSubgraph(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeExprRange: String,
    nodeFraction: Map[String, String]
  ) = Action { implicit request =>
    val (from, to) = dateUtils.parseTimeRange(timeRange)
    val (timeExprFrom, timeExprTo) = dateUtils.parseTimeRange(timeExprRange)
    val facets = Facets(fullText, generic, entities, from, to, timeExprFrom, timeExprTo)
    val sizes = nodeFraction.mapValues(_.toInt)

    val blacklistedIds = entityService.getBlacklisted()(currentDataset).map(_.id)
    val Network(nodes, relations) = networkService.createNetwork(facets, sizes, blacklistedIds)(currentDataset)

    if (nodes.isEmpty) {
      Ok(Json.obj("entities" -> List[JsObject](), "relations" -> List[JsObject]())).as("application/json")
    } else {
      val graphEntities = nodesToJson(nodes)
      // Ignore relations that connect blacklisted nodes
      val graphRelations = relations.filterNot { case Relationship(source, target, _) => blacklistedIds.contains(source) && blacklistedIds.contains(target) }

      Ok(Json.obj("entities" -> graphEntities, "relations" -> graphRelations)).as("application/json")
    }
  }

  /**
   * Adds new nodes to the current network matching the given search query.
   *
   * @param fullText match documents that contain the given expression in the document body.
   * @param generic a map linking from document metadata keys to a list of instances for this metadata.
   * @param entities a list of entity ids that should occur in the document.
   * @param timeRange a string representing a time range for the document creation date.
   * @param timeExprRange a string representing a time range for the document time expression.
   * @param currentNetwork the entity ids of the current network.
   * @param nodes new entities to be added to the network.
   * @return a network consisting of the nodes and relationships of the created co-occurrence network.
   */
  def addNodes(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeExprRange: String,
    currentNetwork: List[Long],
    nodes: List[Long]
  ) = Action { implicit request =>
    val (from, to) = dateUtils.parseTimeRange(timeRange)
    val (timeExprFrom, timeExprTo) = dateUtils.parseTimeRange(timeExprRange)
    val facets = Facets(fullText, generic, entities, from, to, timeExprFrom, timeExprTo)

    val Network(buckets, relations) = networkService.induceNetwork(facets, currentNetwork, nodes)(currentDataset)

    Ok(Json.obj("entities" -> nodesToJson(buckets), "relations" -> relations)).as("application/json")
  }

  // TODO: Use json writer and reader to minimize parameter in a case class Facets
  /**
   * Returns entities co-occurring with the given entity matching the search query.
   *
   * @param fullText match documents that contain the given expression in the document body.
   * @param generic a map linking from document metadata keys to a list of instances for this metadata.
   * @param entities a list of entity ids that should occur in the document.
   * @param timeRange a string representing a time range for the document creation date.
   * @param timeExprRange a string representing a time range for the document time expression.
   * @param currentNetwork the entity ids of the current network.
   * @param focalNode the entity id.
   * @return co-occurring entities with the given entity.
   */
  def getNeighbors(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeExprRange: String,
    currentNetwork: List[Long],
    focalNode: Long
  ) = Action { implicit request =>
    // TODO Duplicated code to parse facets
    val (from, to) = dateUtils.parseTimeRange(timeRange)
    val (timeExprFrom, timeExprTo) = dateUtils.parseTimeRange(timeExprRange)
    val facets = Facets(fullText, generic, entities, from, to, timeExprFrom, timeExprTo)

    // TODO: we don't need to add the blacklist as exclude when we use getById.contains
    val blacklistedIds = entityService.getBlacklisted()(currentDataset).map(_.id)
    val nodes = networkService.getNeighbors(facets, focalNode, numberOfNeighbors, blacklistedIds ++ currentNetwork)(currentDataset)

    val neighbors = nodesToJson(nodes)
    Ok(Json.toJson(neighbors)).as("application/json")
  }

  private def nodesToJson(nodes: List[NodeBucket])(implicit request: Request[AnyContent]): List[JsObject] = {
    val ids = nodes.map(_.id)
    val nodeIdToEntity = entityService.getByIds(ids)(currentDataset).map(e => e.id -> e).toMap

    val typesToId = entityService.getTypes()(currentDataset)
    nodes.collect {
      // Only add node if it is not blacklisted
      case NodeBucket(id, count) if nodeIdToEntity.contains(id) =>
        val node = nodeIdToEntity(id)
        Json.obj(
          "id" -> id,
          "label" -> node.name,
          "count" -> count,
          "type" -> node.entityType.toString,
          "group" -> typesToId(node.entityType)
        )
    }
  }

  /** Marks the entities associated with the given ids as blacklisted. */
  def blacklistEntitiesById(ids: List[Long]) = Action { implicit request =>
    Ok(Json.obj("result" -> entityService.blacklist(ids)(currentDataset))).as("application/json")
  }

  /**
   * Merges multiple nodes in a given focal node.
   *
   * @param focalId the central entity id.
   * @param duplicates entity ids referring to similar textual mentions of the focal id.
   */
  def mergeEntitiesById(focalId: Long, duplicates: List[Long]) = Action { implicit request =>
    entityService.merge(focalId, duplicates)(currentDataset)
    Ok("success").as("Text")
  }

  /** Changes the name of the entity corresponding to the given entity id. */
  def changeEntityNameById(id: Long, newName: String) = Action { implicit request =>
    Ok(Json.obj("result" -> entityService.changeName(id, newName)(currentDataset))).as("application/json")
  }

  /** Changes the type of the entity corresponding to the given entity id. */
  def changeEntityTypeById(id: Long, newType: String) = Action { implicit request =>
    Ok(Json.obj("result" -> entityService.changeType(id, newType)(currentDataset))).as("application/json")
  }
}
