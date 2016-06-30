package controllers

import javax.inject.Inject

import model.faceted.search.{FacetedSearch, MetaDataBucket, NodeBucket}
import model.{Document, Entity, EntityType}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, Controller, Results}
import scalikejdbc._

/**
  * Created by flo on 6/20/2016.
  */
class EntityController @Inject extends Controller {
  implicit val session = AutoSession

  /**
    * get the entities, frequency to given type
    *
    * @param entityType entity type
    * @return
    * an array of entity names and entity frequency
    * combined
    */
  def getEntitiesByType(entityType: String) = Action {
    val entities = Entity.getOrderedByFreqDesc(EntityType.withName(entityType), 50).map(x => Json.obj("id" -> x.id, "name" -> x.name, "freq" -> x.frequency))
    Results.Ok(Json.toJson(entities)).as("application/json")
  }

  /**
    * Gets document counts for entities corresponding to their id's matching the query
    * @param fullText Full text search term
    * @param facets mapping of metadata key and a list of corresponding tags
    * @return list of matching entity id's and document count
    */
  def getEntities(fullText: Option[String], facets: Map[String, List[String]]) = Action {
    val res = FacetedSearch.aggregateEntities(None, Map(), 50).buckets.map(x => x match {
      case NodeBucket(id, count) => Json.obj("key" -> id, "count" -> count)
      case _ => Json.obj()
    })

    Results.Ok(Json.toJson(res)).as("application/json")
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
    * @param filter List of entity id's
    * @return number of documents containing given entities
    */
  def getEntitiesDocCountWithFilter(filter: List[(Long)]) = Action {
    //TODO: prob. need a distinct
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
