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
// to read files

// scalastyle:off

import model.EntityType
import model.faceted.search.{FacetedSearch, Facets, NodeBucket}
import play.api.Logger
import play.api.libs.json.Writes._
import play.api.libs.json.{JsArray, JsObject, Json, Writes}
import play.api.mvc.{Action, Controller}
import util.TimeRangeParser
//import scalikejdbc._

import scalikejdbc._

import scala.collection.mutable

/*
    This class encapsulates all functionality for the
    network graph.
*/
class NetworkController @Inject extends Controller {
  implicit val session = AutoSession

  // TODO: fetch entity types from backend API

  /**
   * this implicit writes allows us to serialize tuple4
   * see http://stackoverflow.com/questions/30921821/play-scala-json-writer-for-seq-of-tuple
   */
  implicit def tuple4Writes[A, B, C, D](implicit a: Writes[A], b: Writes[B], c: Writes[C], d: Writes[D]): Writes[(A, B, C, D)] = new Writes[(A, B, C, D)] {
    def writes(tuple: (A, B, C, D)) = JsArray(Seq(
      a.writes(tuple._1),
      b.writes(tuple._2),
      c.writes(tuple._3),
      d.writes(tuple._4)
    ))
  }

  implicit def tuple3Writes[A, B, C](implicit a: Writes[A], b: Writes[B], c: Writes[C]): Writes[(A, B, C)] = new Writes[(A, B, C)] {
    def writes(tuple: (A, B, C)) = JsArray(Seq(
      a.writes(tuple._1),
      b.writes(tuple._2),
      c.writes(tuple._3)
    ))
  }

  implicit def tuple2Writes[A, B](implicit a: Writes[A], b: Writes[B]): Writes[(A, B)] = new Writes[(A, B)] {
    def writes(tuple: (A, B)) = JsArray(Seq(
      a.writes(tuple._1),
      b.writes(tuple._2)
    ))
  }

  /**
   * the strings for different types
   * !! These are database specific !!
   */
  val locationIdentifier = "LOC"
  val orgIdentifier = "ORG"
  val personIdentifier = "PER"
  val miscIdentifier = "MISC"

  /**
   * This constant is multiplied with the amount of requested nodes in the getgraphdata method
   * to specify the returned relationships, e.g. if the amount is 10 and the multiplier is 1.5,
   * 10 nodes with 15 links between them are returned.
   */
  val relationshipMultiplier = 1.5
  /**
   * This constant is used when loading an ego network and specifies how many relationships
   * between the neighbors of the ego node are shown (i.e. number of links excluding the links
   * of the ego node).
   */
  val neighborRelCount = 5

  /**
   * Knoten der gerade anfokussiert wird
   */
  var focusId: Long = -1

  /**
   * Map cacht Knoten deren DoI-Wert schonmal berechnet wurden. Wird mit dem Beginn jedes Guidance-Schrittes zurückgesetzt.
   */
  var cachedDoIValues: mutable.HashMap[(Long, Long), Double] = new mutable.HashMap[(Long, Long), Double]();

  var cachedEdgeFreq: mutable.HashMap[(Long, Long), Int] = new mutable.HashMap[(Long, Long), Int]();

  var cachedDistanceValues: mutable.HashMap[Long, Int] = new mutable.HashMap[Long, Int]();

  //var lastFound : List[(Long,Long)] = List()
  //var distToFocus = 0

  /**
   * If leastOrMostFrequent == 0:
   * Returns entities with the highest frequency and their relationships with
   * frequencies in the intervall ["minEdgeFreq", "maxEdgeFreq"].
   * If leastOrMostFrequent == 1:
   * Choose the entities with the lowest frequency.
   *
   * "amountOfType" tells how many nodes of which type shall be selected for the graph.
   * amountOfType[0] = contries/cities, amountOfType[1] = organizations,
   * amountOfType[2] = persons, amountOfType[3] = miscellaneous.
   */
  def getGraphData(leastOrMostFrequent: Int, amountOfType: List[Int], minEdgeFreq: Int, maxEdgeFreq: Int) = Action {
    // a list of tuples of id, name, frequency and type (an "entity")
    var entities: List[(Long, String, Int, String)] = List()
    // a list of tuples of id, source, target and frequency (a "relation")
    var relations: List[(Long, Long, Long, Int)] = List()

    val sorting = if (leastOrMostFrequent == 0) sqls"desc" else sqls"asc"

    val locationCount = amountOfType(0)
    val orgCount = amountOfType(1)
    val personCount = amountOfType(2)
    val miscCount = amountOfType(3)
    var amount = 0
    for (a <- amountOfType)
      amount += a

    // get locationCount of type location
    entities = sql"""(SELECT id, name, type, frequency
          FROM entity
          WHERE type = ${locationIdentifier}
          ORDER BY frequency ${sorting}
          LIMIT ${locationCount})
          UNION
          (SELECT id, name, type, frequency
          FROM entity
          WHERE type = ${orgIdentifier}
          ORDER BY frequency ${sorting}
          LIMIT ${orgCount})
          UNION
          (SELECT id, name, type, frequency
          FROM entity
          WHERE type = ${personIdentifier}
          ORDER BY frequency ${sorting}
          LIMIT ${personCount})
          UNION
          (SELECT id, name, type, frequency
          FROM entity
          WHERE type = ${miscIdentifier}
          ORDER BY frequency ${sorting}
          LIMIT ${miscCount})"""
      .map(rs => (rs.long("id"), rs.string("name"), rs.int("frequency"), rs.string("type")))
      .list()
      .apply()

    relations = sql"""SELECT DISTINCT ON (id, frequency) id, entity1, entity2, frequency
        FROM relationship
        WHERE entity1 IN (${entities.map(_._1)})
        AND entity2 IN (${entities.map(_._1)})
        AND frequency >= ${minEdgeFreq}
        AND frequency <= ${maxEdgeFreq}
        ORDER BY frequency ${sorting}
        LIMIT ${amount * relationshipMultiplier}"""
      .map(rs => (rs.long("id"), rs.long("entity1"), rs.long("entity2"), rs.int("frequency")))
      .list()
      .apply()

    val result = new JsObject(Map(("nodes", Json.toJson(entities)), ("links", Json.toJson(relations))))
    Ok(Json.toJson(result)).as("application/json")
  }

  /**
   * Returns the assosciated Id with the given name
   *
   * @param name
   * @return
   */
  def getIdsByName(name: String) = Action {
    Ok(Json.obj("ids" -> model.Entity.getByName(name).map(_.id))).as("application/json")
  }

  /**
   *
   * @param entities list of entity id's you want relations for
   * @param minEdgeFreq minimun Edge Frequency
   * @param maxEdgeFreq maximum Edge Frequency
   * @return
   */
  def getRelations(entities: List[Long], minEdgeFreq: Int, maxEdgeFreq: Int) = Action {
    val relations = sql"""SELECT DISTINCT ON (id, frequency) id, entity1, entity2, frequency
        FROM relationship
        WHERE entity1 IN (${entities})
        AND entity2 IN (${entities})
        AND frequency >= ${minEdgeFreq}
        AND frequency <= ${maxEdgeFreq}
        ORDER BY frequency DESC
        LIMIT 100"""
      .map(rs => (rs.long("id"), rs.long("entity1"), rs.long("entity2"), rs.int("frequency")))
      .list()
      .apply()

    Ok(Json.toJson(relations)).as("application/json")
  }

  def induceSubgraph(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String,
    size: Int,
    filter: List[Long]
  ) = Action {
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val facets = Facets(fullText, generic, entities, times.from, times.to)
    var newSize = size
    if (filter.nonEmpty) newSize = filter.length
    val res = FacetedSearch.induceSubgraph(facets, newSize)
    val subgraphEntities = res._1.map {
      case NodeBucket(id, count) => Json.obj("id" -> id, "count" -> count)
      case _ => Json.obj()
    }

    Ok(Json.toJson(Json.obj("entities" -> subgraphEntities, "relations" -> res._2))).as("application/json")
  }

  /**
   * deletes an entity from the graph by its id
   *
   * @param id the id of the entity to delete
   * @return if the deletion succeeded
   */
  def deleteEntityById(id: Long) = Action {
    Ok(Json.obj("result" -> model.Entity.delete(id))).as("application/json")
  }

  /**
   * merge all entities into one entity represented by the focalId
   *
   * @param focalid the entity to merge into
   * @param ids     the ids of the entities which are duplicates of
   *                the focal entity
   * @return if the merging succeeded
   */
  def mergeEntitiesById(focalid: Int, ids: List[Long]) = Action {
    Ok(Json.obj("result" -> model.Entity.merge(focalid, ids))).as("application/json")
  }

  /**
   * change the entity name by a new name of the given Entity
   *
   * @param id      the id of the entity to change
   * @param newName the new name of the entity
   * @return if the change succeeded
   */
  def changeEntityNameById(id: Long, newName: String) = Action {
    Ok(Json.obj("result" -> model.Entity.changeName(id, newName))).as("application/json")
  }

  /**
   * change the entity type by a new type
   *
   * @param id      the id of the entity to change
   * @param newType the new type of the entity
   * @return if the change succeeded
   */
  def changeEntityTypeById(id: Long, newType: String) = Action {
    Ok(Json.obj("result" -> model.Entity.changeType(id, EntityType.withName(newType)))).as("application/json")
  }

  // scalastyle:off
  /**
   * Returns the nodes and edges of the ego network of the node with id "id".
   * Which and how many nodes and edges are to be selected is defined by the
   * parameters "amountOfType" and "existingNodes".
   * If leastOrMostFrequent == 0 it is tried to get those with the highest frequency.
   * If leastOrMostFrequent == 1 it is tried to get those with the lowest frequency.
   *
   * "amountOfType" tells how many nodes of which type shall be selected for the ego
   * network. amountOfType[0] = contries/cities, amountOfType[1] = organizations,
   * amountOfType[2] = persons, amountOfType[3] = miscellaneous.
   *
   * "existingNodes" the ids of the nodes that are already in the ego network.
   */
  def getEgoNetworkData(leastOrMostFrequent: Int, id: Long, amountOfType: List[Int], existingNodes: List[Long]) = Action {
    // a list of tuples of id, name, frequency and type (an "entity")
    var entities: List[(Long, String, Int, String)] = List()
    // a list of tuples of id, source, target and frequency (a "relation")
    var relations: List[(Long, Long, Long, Int)] = List()

    val sorting = if (leastOrMostFrequent == 0) sqls"DESC" else sqls"ASC"

    val locationCount = amountOfType.head
    val orgCount = amountOfType(1)
    val personCount = amountOfType(2)
    val miscCount = amountOfType(3)

    val existingNodesForSql = if (existingNodes.isEmpty) List(-1) else existingNodes
    // get locationCount of type location
    relations = sql"""(SELECT relationship.id, entity1, entity2, relationship.frequency
                                        FROM relationship, entity
                                        WHERE entity1 = ${id}
                                        AND entity2 NOT IN (${existingNodesForSql})
                                        AND entity2 = entity.id
                                        AND type = ${locationIdentifier}
                                        ORDER BY relationship.frequency ${sorting}
                                        LIMIT ${locationCount})

                                        UNION
          (SELECT relationship.id, entity1, entity2, relationship.frequency
                                                  FROM relationship, entity
                                                  WHERE entity1 = ${id}
                                                  AND entity2 NOT IN (${existingNodesForSql})
                                                  AND entity2 = entity.id
                                                  AND type = ${orgIdentifier}
                                                  ORDER BY relationship.frequency ${sorting}
                                                  LIMIT ${orgCount})
          UNION
          (SELECT relationship.id, entity1, entity2, relationship.frequency
                                                  FROM relationship, entity
                                                  WHERE entity1 = ${id}
                                                  AND entity2 NOT IN (${existingNodesForSql})
                                                  AND entity2 = entity.id
                                                  AND type = ${personIdentifier}
                                                  ORDER BY relationship.frequency ${sorting}
                                                  LIMIT ${personCount})
                                                                                          UNION
          (SELECT relationship.id, entity1, entity2, relationship.frequency
                                                  FROM relationship, entity
                                                  WHERE entity1 = ${id}
                                                  AND entity2 NOT IN (${existingNodesForSql})
                                                  AND entity2 = entity.id
                                                  AND type = ${miscIdentifier}
                                                  ORDER BY relationship.frequency ${sorting}
                                                  LIMIT ${miscCount})"""
      .map(rs => (rs.long("id"), rs.long("entity1"), rs.long("entity2"), rs.int("frequency")))
      .list()
      .apply()

    // IF the relation list IS NOT empty
    if (relations.nonEmpty) {
      val relationConcat = relations.map(_._2) ++ relations.map(_._3)
      entities = sql"""SELECT id, name, type, frequency
                           FROM entity
                           WHERE id IN (${relationConcat})"""
        .map(rs => (rs.long("id"), rs.string("name"), rs.int("frequency"), rs.string("type")))
        .list()
        .apply()

    }
    // Get the neighborRelCount most/least relevant relations between neighbors of the node with the id "id".
    relations = List.concat(relations, getNeighborRelations(sorting, entities, neighborRelCount, id))

    val result = new JsObject(Map(("nodes", Json.toJson(entities)), ("links", Json.toJson(relations))))

    Ok(Json.toJson(result)).as("application/json")
  }
  // scalastyle:off

  /**
   * Returns a list with "amount" relations between neighbors of the node with the id "id".
   */
  private def getNeighborRelations(
    sorting: SQLSyntax,
    entities: List[(Long, String, Int, String)],
    amount: Int,
    id: Long
  ): List[(Long, Long, Long, Int)] = {
    if (entities.nonEmpty) {
      sql"""SELECT DISTINCT ON(id, frequency) id, entity1, entity2, frequency
          FROM relationship
          WHERE entity1 IN (${entities.map(_._1)})
          AND entity2 IN (${entities.map(_._1)})
          ORDER BY frequency ${sorting}
          LIMIT ${amount}"""
        .map(rs => (rs.long("id"), rs.long("entity1"), rs.long("entity2"), rs.int("frequency")))
        .list
        .apply()
    } else {
      List()
    }
  }

  /**
   *
   * @param edgeId Kante, Tuple der Form (source,target)
   * @return the computed degree of interest value of a given edge
   */
  def doI(edgeId: (Long, Long)): Double = {
    cachedDoIValues.get(edgeId).get
  }

  /**
   *
   * @param focusId anfokussierter Knoten
   * @return sendet die Kanten+Knoten an den Benutzer
   */
  def getGuidanceNodes(focusId: Long) = Action {
    implicit val session = AutoSession

    Logger.info("start guidance")
    this.focusId = focusId
    cachedDoIValues.clear()
    cachedDistanceValues.clear()
    val k = 20
    var edgeArr = new Array[(Long, Long)](k) //(id, source, target, frequency)
    var usedNodes = new mutable.HashSet[Long]()
    usedNodes += focusId

    var pq = mutable.PriorityQueue[(Long, Long)]()(Ordering.by[(Long, Long), Double]((x) => doI(x._1, x._2)))
    pq ++= getEdges(focusId)

    for (i <- 0 until k) { //edgeArr.map ??
      val edge = pq.dequeue()
      Logger.info("E:" + edge._1 + "," + edge._2 + " V:" + doI(edge))

      edgeArr(i) = edge
      //      if ( ! usedNodes.contains(edge._1)){
      //        usedNodes += edge._1
      //        pq ++= getEdges(edge._1)
      //      } else
      if (!usedNodes.contains(edge._2)) {
        usedNodes += edge._2
        if (i < k / 2) {
          pq ++= getEdges(edge._2)
        }
      }
      Logger.info(edgeArr.toString)
    }

    //bestimme Namen, Frequenz und Typ der Knoten
    Logger.info("ggN SQL: SELECT id, name, type, frequency FROM entity WHERE id IN ")
    val nodes = sql"""SELECT id, name, type, frequency FROM entity WHERE id IN ($usedNodes)"""
      .map(rs => (rs.long("id"), rs.string("name"), rs.int("frequency"), rs.string("type"))).list().apply()

    val result = new JsObject(Map(("nodes", Json.toJson(nodes)), ("links", Json.toJson(edgeArr.map(e => (e._1, e._2, cachedEdgeFreq.apply(e))))))) //TODO Kantenattribute müssen auch zurückgegeben werden

    Ok(Json.toJson(result)).as("application/json")
  }

  /**
   *
   * @param nodeId Knoten
   * @return die an den Knoten angrenzenden Kanten in einer Liste
   */
  private def getEdges(nodeId: Long): List[(Long, Long)] = {
    import scalikejdbc._
    implicit val session = AutoSession

    var distToFocus = cachedDistanceValues.getOrElse(nodeId, -1) //Wenn der Knoten nicht in der Map liegt, muss es sich um dem Fokus handeln, also dist=0
    distToFocus += 1
    val result = sql"""SELECT entity1, entity2, relationship.frequency AS efreq, et1.frequency AS n1freq, et2.frequency AS n2freq
          FROM relationship LEFT JOIN entity AS et1 ON et1.id = relationship.entity1
          LEFT JOIN entity AS et2 ON et2.id = relationship.entity2 WHERE entity1 = $nodeId ORDER BY relationship.frequency DESC LIMIT 300""".map(rs => {
      (rs.long(1), rs.long(2), rs.int(3), rs.int(4), rs.int(5)) //TODO Limit verändert sich
    })
      .toList().apply().filter(x => cachedDistanceValues.getOrElse(x._2, distToFocus + 1) > distToFocus).map(x =>
        ((x._1, x._2), (x._3 * x._3 * x._3) / (x._4 * x._5), x._3))

    cachedDistanceValues ++= result.map(x => (x._1._2, distToFocus))
    cachedEdgeFreq ++= result.map(x => (x._1, x._3))
    cachedDoIValues ++= result.map(x => (x._1, {
      val alpha = 1
      val beta = 1
      val gamma = 0

      val API = x._2
      val D = -(1 - scala.math.pow(0.5, distToFocus) * x._2)
      val UI = 0
      API * alpha + D * beta + UI * gamma
    }))

    result.map(_._1)
  }

}

