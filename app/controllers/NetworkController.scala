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

import model.EntityType._
import model.faceted.search.{ FacetedSearch, Facets, NodeBucket }
import model.{ Entity, EntityType }
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.{ Action, Controller }
import util.SessionUtils.currentDataset
import util.TimeRangeParser

// scalastyle:off
import util.TupleWriters._
// scalastyle:off

/**
  * This class encapsulates all functionality for the
  * network graph.
  */
class NetworkController @Inject extends Controller {

  /**
    * Returns the associated Id with the given name
    *
    * @param name
    * @return
    */
  def getIdsByName(name: String) = Action { implicit request =>
    Ok(Json.obj("ids" -> Entity.fromDBName(currentDataset).getByName(name).map(_.id))).as("application/json")
  }

  def induceSubgraph(
                      fullText: List[String],
                      generic: Map[String, List[String]],
                      entities: List[Long],
                      timeRange: String,
                      nodeFraction: Map[String, String],
                      filter: List[Long]
                    ) = Action { implicit request =>
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val facets = Facets(fullText, generic, entities, times.from, times.to)
    // var newSize = size
    // if (filter.nonEmpty) newSize = filter.length

    val sizes = nodeFraction.map { case (t, s) => withName(t) -> s.toInt }
    val (buckets, relations) = FacetedSearch.
      fromIndexName(currentDataset).
      induceSubgraph(facets, sizes)
    val nodes = buckets.collect { case a @ NodeBucket(_, _) => a }

    if (nodes.isEmpty) {
      Ok(Json.toJson(Json.obj("entities" -> List[JsObject](), "relations" -> List[JsObject]()))).as("application/json")
    } else {
      val ids = nodes.map(_.id)
      val nodeIdToEntity = Entity.fromDBName(currentDataset).getByIds(ids).map(e => e.id -> e).toMap

      val graphEntities = nodes.collect {
        // Ignore blacklisted nodes
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
      // Ignore relations that connect blacklisted nodes
      val graphRelations = relations.filter { case (from, to, _) => nodeIdToEntity.contains(from) && nodeIdToEntity.contains(to) }

      val types = Json.obj(
        Person.toString -> Person.id,
        Organization.toString -> Organization.id,
        Location.toString -> Location.id,
        Misc.toString -> Misc.id
      )
      Ok(Json.toJson(Json.obj("entities" -> graphEntities, "relations" -> graphRelations, "types" -> types))).as("application/json")
    }
  }

  /**
    * deletes an entity from the graph by its id
    *
    * @param id the id of the entity to delete
    * @return if the deletion succeeded
    */
  def deleteEntityById(id: Long) = Action { implicit request =>
    Ok(Json.obj("result" -> Entity.fromDBName(currentDataset).delete(id))).as("application/json")
  }

  /**
    * merge all entities into one entity represented by the focalId
    *
    * @param focalid the entity to merge into
    * @param ids     the ids of the entities which are duplicates of
    *                the focal entity
    * @return if the merging succeeded
    */
  def mergeEntitiesById(focalid: Int, ids: List[Long]) = Action { implicit request =>
    Ok(Json.obj("result" -> Entity.fromDBName(currentDataset).merge(focalid, ids))).as("application/json")
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