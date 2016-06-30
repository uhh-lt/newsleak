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


import model.EntityType
import play.api.libs.json.{JsArray, JsObject, Json, Writes}
import play.api.mvc.{Action, Controller}
import scalikejdbc._

/*
    This class encapsulates all functionality for the
    network graph.
*/
object NetworkController extends Controller {
  implicit val session = AutoSession

  /**
    * this implicit writes allows us to serialize tuple4
    * see http://stackoverflow.com/questions/30921821/play-scala-json-writer-for-seq-of-tuple
    */
  implicit def tuple4Writes[A, B, C, D](implicit a: Writes[A], b: Writes[B], c: Writes[C], d: Writes[D]): Writes[Tuple4[A, B, C, D]] = new Writes[Tuple4[A, B, C, D]] {
    def writes(tuple: Tuple4[A, B, C, D]) = JsArray(Seq(a.writes(tuple._1),
      b.writes(tuple._2),
      c.writes(tuple._3),
      d.writes(tuple._4)))
  }

  //TODO: fetch entity types from backend API
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
      .map(rs => (rs.long("id"), rs.string("name"),  rs.int("frequency"), rs.string("type")))
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
      .map(rs => (rs.long("id"),rs.long("entity1"), rs.long("entity2"), rs.int("frequency")))
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

    val existingNodesForSql = if(existingNodes.isEmpty) List(-1) else existingNodes
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
        if(relations.nonEmpty ){
          val relationConcat = relations.map(_._2) ++ relations.map(_._3)
          entities = sql"""SELECT id, name, type, frequency
                           FROM entity
                           WHERE id IN (${relationConcat})"""
            .map(rs => (rs.long("id"), rs.string("name"),  rs.int("frequency"), rs.string("type")))
            .list()
            .apply()

        }
        // Get the neighborRelCount most/least relevant relations between neighbors of the node with the id "id".
        relations = List.concat(relations, getNeighborRelations(sorting, entities, neighborRelCount, id))

        val result = new JsObject(Map(("nodes", Json.toJson(entities)) , ("links", Json.toJson(relations))))

        Ok(Json.toJson(result)).as("application/json")
    }

    /**
     * Returns a list with "amount" relations between neighbors of the node with the id "id".
     */
    private def getNeighborRelations(sorting: SQLSyntax,
                                    entities : List[(Long, String, Int, String)],
                                    amount: Int, 
                                    id: Long) : List[(Long, Long, Long, Int)] = {
      if(entities.nonEmpty){
          val relations: List[(Long, Long, Long, Int)] = sql"""SELECT DISTINCT ON(id, frequency) id, entity1, entity2, frequency
          FROM relationship
          WHERE entity1 IN (${entities.map(_._1)})
          AND entity2 IN (${entities.map(_._1)})
          ORDER BY frequency ${sorting}
          LIMIT ${amount}"""
      .map(rs => (rs.long("id"), rs.long("entity1"), rs.long("entity2"), rs.int("frequency")))
      .list // single, list, traversable
      .apply()
    return relations
  }
  List()
}

}
