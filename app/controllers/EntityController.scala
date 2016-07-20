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

import model.faceted.search.{ FacetedSearch, Facets, MetaDataBucket, NodeBucket }
import model.{ Document, Entity, EntityType }
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.{ Action, Controller, Results }
import scalikejdbc._
import util.TimeRangeParser

/**
 * Created by flo on 6/20/2016.
 */
class EntityController @Inject extends Controller {
  implicit val session = AutoSession

  private val defaultFetchSize = 50

  /**
   * get the entities, frequency to given type
   *
   * @param entityType entity type
   * @return
   * an array of entity names and entity frequency
   * combined
   */
  def getEntitiesByType(entityType: String) = Action {
    val entities = Entity.getOrderedByFreqDesc(EntityType.withName(entityType), defaultFetchSize)
      .map(x => Json.obj("id" -> x.id, "name" -> x.name, "freq" -> x.frequency))
    Results.Ok(Json.toJson(entities)).as("application/json")
  }

  /**
   * Get all entity types
   * @return list of entity types
   */
  def getEntityTypes = Action {
    Results.Ok(Json.toJson(Entity.getTypes().map(_.toString))).as("application/json")
  }

  /**
   * Gets document counts for entities corresponding to their id's matching the query
   * @param fullText Full text search term
   * @param generic   mapping of metadata key and a list of corresponding tags
   * @param entities list of entity ids to filter
   * @param timeRange string of a time range readable for [[TimeRangeParser]]
   * @return list of matching entity id's and document count
   */
  def getEntities(fullText: Option[String], generic: Map[String, List[String]], entities: List[Long], timeRange: String) = Action {
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val facets = Facets(fullText, generic, entities, times.from, times.to)
    val entitiesRes = FacetedSearch.aggregateEntities(facets, defaultFetchSize).buckets.map(x => x match {
      case NodeBucket(id, count) => (id, count)
      case _ => (0, 0)
    })
    var result: List[JsObject] = List()
    if (entitiesRes.nonEmpty) {
      result =
        sql"""SELECT * FROM entity
          WHERE id IN (${entitiesRes.map(_._1)}) AND NOT isblacklisted
          ORDER BY frequency DESC LIMIT 50"""
          .map(Entity(_))
          .list // single, list, traversable
          .apply()
          .map(x => Json.obj("id" -> x.id, "name" -> x.name, "type" -> x.entityType, "freq" -> x.frequency))
    }
    Results.Ok(Json.toJson(result)).as("application/json")
  }

  /**
   * get the entities, frequency to given type using an offset
   *
   * @param entityType entity type
   * @param offset   offset for beginning index
   * @return
   * an array of entity names and entity frequency
   * combined
   */
  def getEntitiesWithOffset(entityType: String, offset: Int) = Action {
    val result =
      sql"""SELECT * FROM entity
          WHERE type = ${entityType} AND NOT isblacklisted
          ORDER BY frequency DESC LIMIT 50 OFFSET ${offset}"""
        .map(Entity(_))
        .list // single, list, traversable
        .apply()
        .map(x => Json.obj("id" -> x.id, "name" -> x.name, "freq" -> x.frequency))

    Results.Ok(Json.toJson(result)).as("application/json")

  }

  /**
   * get the entities and count of corresponding documents to given type
   *
   * @param entityType entity type
   * @return an array of entity names and document count
   *         combined
   */
  def getEntitiesDocCount(entityType: String) = Action {
    val result =
      sql"""SELECT e.id, e.name, count(d.docid) AS count
                  FROM documententity d, entity e
                  WHERE e.type = ${entityType} AND d.entityid = e.id
                  GROUP BY e.id ORDER BY count DESC LIMIT 50;"""
        .map(rs => Json.obj("id" -> rs.long("id"), "name" -> rs.string("name"), "freq" -> rs.int("count")))
        .list()
        .apply()

    Results.Ok(Json.toJson(result)).as("application/json")
  }

  /**
   * get the entities and count of corresponding documents to given type
   * using an offset
   *
   * @param entityType entity type
   * @param offset   offset for beginning index
   * @return an array of entity names and document count
   *         combined
   */
  def getEntitiesDocCountWithOffset(entityType: String, offset: Int) = Action {
    val result =
      sql"""SELECT e.id, e.name, count(d.docid) AS count
                  FROM documententity d, entity e
                  WHERE e.type = ${entityType} AND d.entityid = e.id
                  GROUP BY e.id ORDER BY count DESC LIMIT 50 OFFSET ${offset};"""
        .map(rs => Json.obj("id" -> rs.long("id"), "name" -> rs.string("name"), "freq" -> rs.int("count")))
        .list()
        .apply()

    Results.Ok(Json.toJson(result)).as("application/json")
  }

  /**
   * Number of Documents containing given entity id's
   *
   * @param filter List of entity id's
   * @return number of documents containing given entities
   */
  def getEntitiesDocCountWithFilter(filter: List[(Long)]) = Action {
    // TODO: prob. need a distinct
    val rs =
      sql"""SELECT count(*) FROM
        (SELECT d.docid AS ids, count(d.entityid) AS count_ent
        FROM documententity d
        WHERE d.entityid IN (${filter})
        GROUP BY d.docid
        HAVING count(d.entityid) >= ${filter.length}) foo;"""
        .map(rs => rs.int("count"))
        .single()
        .apply()

    Results.Ok(Json.toJson(rs)).as("application/json")
  }
}
