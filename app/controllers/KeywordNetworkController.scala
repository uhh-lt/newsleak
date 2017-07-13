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

import models.services.{ EntityService, KeywordNetworkService }
import models.{ Facets, KeyTerm, KeywordNetwork, KeywordRelationship }
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.{ Action, AnyContent, Controller, Request }
import scalikejdbc.{ NamedDB, SQL }
import util.DateUtils
import util.SessionUtils.currentDataset

/**
 * Provides network related actions.
 * @param entityService the service for entity backend operations.
 * @param keywordNetworkService the service for KeywordNetwork backend operations.
 * @param dateUtils common helper for date and time operations.
 */
class KeywordNetworkController @Inject() (
    entityService: EntityService,
    keywordNetworkService: KeywordNetworkService,
    dateUtils: DateUtils
) extends Controller {

  private val db = (index: String) => NamedDB(Symbol(index))
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
  def getNeighborCountsKeyword(
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

    val res = keywordNetworkService.getNeighborCountsPerTypeKeyword(facets, nodeId)(currentDataset)
    val counts = res.map { case (t, c) => Json.obj("type" -> t, "count" -> c) }
    Ok(Json.toJson(counts)).as("application/json")
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
  def induceSubgraphKeyword(
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

    // val blacklistedKeywords = getBlacklistedKeywords()(currentDataset).map(_.term)
    // val blacklistedKeywords = entityService.getBlacklisted()(currentDataset).map(_.name)
    val blacklistedKeywords = List()
    val KeywordNetwork(nodes, relations) = keywordNetworkService.createNetworkKeyword(facets, sizes, blacklistedKeywords)(currentDataset)

    if (nodes.isEmpty) {
      Ok(Json.obj("entities" -> List[JsObject](), "relations" -> List[JsObject]())).as("application/json")
    } else {
      val graphEntities = nodesToJsonKeyword(nodes)
      // Ignore relations that connect blacklisted nodes
      val graphRelations = relations.filterNot { case KeywordRelationship(source, target, _) => blacklistedKeywords.contains(source) && blacklistedKeywords.contains(target) }

      Ok(Json.obj("entities" -> graphEntities, "relations" -> graphRelations)).as("application/json")
    }
  }

  /*
  def getBlacklistedKeywords()(index: String): List[KeyTerm] = db(index).readOnly { implicit session =>
    sql"SELECT * FROM terms").map(KeyTerm(_).list.apply()
  }
  */

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
  def addNodesKeyword(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    timeExprRange: String,
    currentNetwork: List[String],
    nodes: List[String]
  ) = Action { implicit request =>
    val (from, to) = dateUtils.parseTimeRange(timeRange)
    val (timeExprFrom, timeExprTo) = dateUtils.parseTimeRange(timeExprRange)
    val facets = Facets(fullText, generic, entities, from, to, timeExprFrom, timeExprTo)

    val KeywordNetwork(buckets, relations) = keywordNetworkService.induceNetworkKeyword(facets, currentNetwork, nodes)(currentDataset)

    Ok(Json.obj("entities" -> nodesToJsonKeyword(buckets), "relations" -> relations)).as("application/json")
  }

  private def nodesToJsonKeyword(nodes: List[KeyTerm])(implicit request: Request[AnyContent]): List[JsObject] = {
    val termsArray: Array[JsObject] = new Array[JsObject](nodes.length)

    for (i <- 0 to nodes.length - 1) termsArray(i) = Json.obj(
      "id" -> i,
      "label" -> nodes(i).term,
      "count" -> nodes(i).score
    )

    val terms: List[JsObject] = termsArray.toList
    terms
  }

  /** Marks the entities associated with the given ids as blacklisted. */
  def blacklistEntitiesByIdKeyword(ids: List[Long]) = Action { implicit request =>
    Ok(Json.obj("result" -> entityService.blacklist(ids)(currentDataset))).as("application/json")
  }

  /** Marks the keywords associated with the given ids as blacklisted. */
  def blacklistKeywordsByIdKeyword(ids: List[Long]) = Action { implicit request =>
    Ok(Json.obj("result" -> entityService.blacklistKeyword(ids)(currentDataset))).as("application/json")
  }

  /**
   * Merges multiple nodes in a given focal node.
   *
   * @param focalId the central entity id.
   * @param duplicates entity ids referring to similar textual mentions of the focal id.
   */
  def mergeEntitiesByIdKeyword(focalId: Long, duplicates: List[Long]) = Action { implicit request =>
    entityService.merge(focalId, duplicates)(currentDataset)
    Ok("success").as("Text")
  }

  /** Changes the name of the entity corresponding to the given entity id. */
  def changeEntityNameByIdKeyword(id: Long, newName: String) = Action { implicit request =>
    Ok(Json.obj("result" -> entityService.changeName(id, newName)(currentDataset))).as("application/json")
  }

  /** Changes the type of the entity corresponding to the given entity id. */
  def changeEntityTypeByIdKeyword(id: Long, newType: String) = Action { implicit request =>
    Ok(Json.obj("result" -> entityService.changeType(id, newType)(currentDataset))).as("application/json")
  }
}
