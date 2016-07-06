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

// to read files
import java.util.StringJoiner

import play.api.Logger
import play.api.Play.current
import play.api.db._
import play.api.libs.json.Writes._
import play.api.libs.json.{JsArray, JsObject, Json, Writes}
import play.api.mvc.{Action, Controller}

import scala.collection.mutable

/*
    This class encapsulates all functionality for the
    network graph.
*/
object NetworkController extends Controller {

    /**
     * this implicit writes allows us to serialize tuple4
     * see http://stackoverflow.com/questions/30921821/play-scala-json-writer-for-seq-of-tuple
     */
    implicit def tuple4Writes[A, B, C, D](implicit a: Writes[A], b: Writes[B], c: Writes[C], d: Writes[D]): Writes[(A, B, C, D)] = new Writes[(A, B, C, D)] {
        def writes(tuple: (A, B, C, D)) = JsArray(Seq(a.writes(tuple._1),
                                                            b.writes(tuple._2),
                                                            c.writes(tuple._3),
                                                            d.writes(tuple._4)))
    }

  /**
     * the strings for different types
     * !! These are database specific !!
     */
    val locationIdentifier   = "LOC"
    val orgIdentifier        = "ORG"
    val personIdentifier     = "PER"
    val miscIdentifier       = "MISC"

    /**
     * This constant is multiplied with the amount of requested nodes in the getgraphdata method
     * to specify the returned relationships, e.g. if the amount is 10 and the multiplier is 1.5,
     * 10 nodes with 15 links between them are returned.
     */
    val relationshipMultiplier  = 1.5
    /**
     * This constant is used when loading an ego network and specifies how many relationships
     * between the neighbors of the ego node are shown (i.e. number of links excluding the links
     * of the ego node).
     */
    val neighborRelCount        = 5


    /**
     * Knoten der gerade anfokussiert wird
     */
    var focusId :Long = -1

    /**
     * Map cacht Knoten deren DoI-Wert schonmal berechnet wurden. Wird mit dem Beginn jedes Guidance-Schrittes zurückgesetzt.
     */
    var cachedDoIValues: mutable.HashMap[(Long,Long),Double] = new mutable.HashMap[(Long,Long),Double] () ;

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
    def getGraphData(leastOrMostFrequent: Int, amountOfType: List[Int], minEdgeFreq: Int, maxEdgeFreq: Int) = Action{
        // a list of tuples of id, name, frequency and type (an "entity")
        var entities  : List[(Long, String, Int, String)]   = List()
        // a list of tuples of id, source, target and frequency (a "relation")
        var relations : List[(Long, Long, Long, Int)]       = List()

        val sorting = if(leastOrMostFrequent == 0) "DESC" else "ASC"

        val locationCount   = amountOfType(0)
        val orgCount        = amountOfType(1)
        val personCount     = amountOfType(2)
        val miscCount       = amountOfType(3)
        var amount = 0
        for(a <- amountOfType)
            amount += a

        DB.withConnection { implicit connection =>
            val stmt = connection.createStatement
            val rs = stmt.executeQuery( // get locationCount of type location
                                        "(SELECT id, name, type, frequency "+
                                        "FROM entity "+
                                        "WHERE type = \'"+locationIdentifier+"\' "+
                                        "ORDER BY frequency "+sorting+" "+
                                        "LIMIT "+locationCount+") "+
                                        // get orgCount of type org
                                        "UNION "+
                                        "(SELECT id, name, type, frequency "+
                                        "FROM entity "+
                                        "WHERE type = \'"+orgIdentifier+"\' "+
                                        "ORDER BY frequency "+sorting+" "+
                                        "LIMIT "+orgCount+") "+
                                        // get personCount of type person
                                        "UNION "+
                                        "(SELECT id, name, type, frequency "+
                                        "FROM entity "+
                                        "WHERE type = \'"+personIdentifier+"\' "+
                                        "ORDER BY frequency "+sorting+" "+
                                        "LIMIT "+personCount+") "+
                                        // get miscCount of type misc
                                        "UNION "+
                                        "(SELECT id, name, type, frequency "+
                                        "FROM entity "+
                                        "WHERE type = \'"+miscIdentifier+"\' "+
                                        "ORDER BY frequency "+sorting+" "+
                                        "LIMIT "+miscCount+")")
            while(rs.next()){
                val id          :Long   = rs.getLong("id")
                val name        :String = rs.getString("name")
                val typ         :String = rs.getString("type")
                val frequency   :Int    = rs.getInt("frequency")

                entities        ::= new Tuple4(id, name, frequency, typ)

            }
        }

        val sj = new StringJoiner(",", "(", ")")
        entities.map(node => sj.add(node._1.toString))
        val entityIdString = sj.toString()

        DB.withConnection { implicit connection =>
            val stmt = connection.createStatement
            val rs = stmt.executeQuery( "SELECT DISTINCT ON (id, frequency) id, entity1, entity2, frequency "+
                                        "FROM relationship "+
                                        "WHERE entity1 IN "+entityIdString+" "+
                                        "AND entity2 IN "+entityIdString+" "+
                                        "AND frequency >= "+minEdgeFreq+" "+
                                        "AND frequency <= "+maxEdgeFreq+" "+
                                        "ORDER BY frequency "+sorting+" "+
                                        "LIMIT "+(amount * relationshipMultiplier).toInt)
            while(rs.next()){
                // id, source, target, frequency
                val id          :Long   = rs.getLong("id")
                val source      :Long   = rs.getLong("entity1")
                val target      :Long   = rs.getLong("entity2")
                val frequency   :Int    = rs.getInt("frequency")
                
                relations       ::= (id, source, target, frequency)

            }
        }

        val result = new JsObject(Map(("nodes", Json.toJson(entities)) , ("links", Json.toJson(relations))))

        Ok(Json.toJson(result)).as("application/json")
    }

	/**
      * Returns the assosciated Id with the given name
    *
    * @param name
      * @return
      */
    def getIdsByName(name: String) = Action
	{
        val entityresults : List[model.Entity] = model.Entity.getByName(name)
    System.out.println(entityresults)
		var entities : List[Long] = List()

		for (e <- entityresults)
		{
			val id:Long = e.id
			entities ::= id
		}

		val result = new JsObject(Map(("ids", Json.toJson(entities))))
		Ok(Json.toJson(result)).as("application/json")
    }

	/**
	  * deletes an entity from the graph by its id
    *
    * @param id the id of the entity to delete
	  * @return if the deletion succeeded
	  */
    def deleteEntityById(id: Long) = Action
	{
		val success:Boolean = model.Entity.delete(id)
		val result = new JsObject(Map(("result", Json.toJson(success))))
		Ok(Json.toJson(result)).as("application/json")
	}

	/**
	  * merge all entities into one entity represented by the focalId
    *
    * @param focalid the entity to merge into
	  * @param ids the ids of the entities which are duplicates of
	  *            the focal entity
	  * @return if the merging succeeded
	  */
    def mergeEntitiesById(focalid: Int, ids: List[Long]) = Action
    {
        val success:Boolean = model.Entity.merge(focalid, ids)

        val result = new JsObject(Map(("result", Json.toJson(success))))
        Ok(Json.toJson(result)).as("application/json")
    }

	/**
	  * change the entity name by a new name of the given Entity
    *
    * @param id the id of the entity to change
	  * @param newName the new name of the entity
	  * @return if the change succeeded
	  */
	def changeEntityNameById(id: Long, newName: String) = Action
	{
		val success:Boolean = model.Entity.changeName(id, newName)

		val result = new JsObject(Map(("result", Json.toJson(success))))
		Ok(Json.toJson(result)).as("application/json")
	}

	/**
	  * change the entity type by a new type
    *
    * @param id the id of the entity to change
	  * @param newType the new type of the entity
	  * @return if the change succeeded
	  */
	def changeEntityTypeById(id: Long, newType: String) = Action
	{
		val newTypeValue = newType match {
			case "PER" => model.EntityType.Person
			case "ORG" => model.EntityType.Organization
			case "LOC" => model.EntityType.Location
			case "MISC" => model.EntityType.Misc
		}

		val success:Boolean = model.Entity.changeType(id, newTypeValue);

		val result = new JsObject(Map(("result", Json.toJson(success))));
		Ok(Json.toJson(result)).as("application/json");
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
    def getEgoNetworkData(leastOrMostFrequent: Int, id: Long, amountOfType: List[Int], existingNodes: List[Long]) = Action{
        // a list of tuples of id, name, frequency and type (an "entity")
        var entities  : List[(Long, String, Int, String)]   = List()
        // a list of tuples of id, source, target and frequency (a "relation")
        var relations : List[(Long, Long, Long, Int)]       = List()

        val sorting = if(leastOrMostFrequent == 0) "DESC" else "ASC"
        val idSJ    = new StringJoiner(",", "(", ")")
        idSJ.setEmptyValue("(-1)")
        existingNodes.map(id => idSJ.add(id.toString()))
        val existingIDString = idSJ.toString()

        val locationCount   = amountOfType(0)
        val orgCount        = amountOfType(1)
        val personCount     = amountOfType(2)
        val miscCount       = amountOfType(3)

        DB.withConnection { implicit connection =>
            val stmt = connection.createStatement
            val rs = stmt.executeQuery( // get locationCount of type location
                                        "(SELECT relationship.id, entity1, entity2, relationship.frequency "+
                                        "FROM relationship, entity "+
                                        "WHERE entity1 = "+id+" "+
                                        "AND entity2 NOT IN "+existingIDString+" "+
                                        "AND entity2 = entity.id "+ 
                                        "AND type = \'"+locationIdentifier+"\' "+
                                        "ORDER BY relationship.frequency "+sorting+" "+
                                        "LIMIT "+locationCount+")"+
                                        // get orgCount of type org
                                        "UNION "+
                                        "(SELECT relationship.id, entity1, entity2, relationship.frequency "+
                                        "FROM relationship, entity "+
                                        "WHERE entity1 = "+id+" "+
                                        "AND entity2 NOT IN "+existingIDString+" "+
                                        "AND entity2 = entity.id "+ 
                                        "AND type = \'"+orgIdentifier+"\' "+
                                        "ORDER BY relationship.frequency "+sorting+" "+
                                        "LIMIT "+orgCount+")"+
                                        // get personCount of type person
                                        "UNION "+
                                        "(SELECT relationship.id, entity1, entity2, relationship.frequency "+
                                        "FROM relationship, entity "+
                                        "WHERE entity1 = "+id+" "+
                                        "AND entity2 NOT IN "+existingIDString+" "+
                                        "AND entity2 = entity.id "+ 
                                        "AND type = \'"+personIdentifier+"\' "+
                                        "ORDER BY relationship.frequency "+sorting+" "+
                                        "LIMIT "+personCount+")"+
                                        // get miscCount of type misc
                                        "UNION "+
                                        "(SELECT relationship.id, entity1, entity2, relationship.frequency "+
                                        "FROM relationship, entity "+
                                        "WHERE entity1 = "+id+" "+
                                        "AND entity2 NOT IN "+existingIDString+" "+
                                        "AND entity2 = entity.id "+ 
                                        "AND type = \'"+miscIdentifier+"\' "+
                                        "ORDER BY relationship.frequency "+sorting+" "+
                                        "LIMIT "+miscCount+")")
            while(rs.next()){
                // id, source, target, frequency
                val id          :Long   = rs.getLong("id")
                val source      :Long   = rs.getLong("entity1")
                val target      :Long   = rs.getLong("entity2")
                val frequency   :Int    = rs.getInt("frequency")
                
                relations ::= new Tuple4(id, source, target, frequency)
            }
        }

        // if the relation list is not empty
        if(relations.size > 0 ){
            val relSJ = new StringJoiner(",", "(", ")")
            relations.map(rel => relSJ.add(rel._2.toString).add(rel._3.toString))
            val relNodesString = relSJ.toString()

            DB.withConnection { implicit connection =>
                val stmt = connection.createStatement
                val rs = stmt.executeQuery( "SELECT id, name, type, frequency "+
                                            "FROM entity "+
                                            "WHERE id IN "+relNodesString)
                while(rs.next()){
                    val id          :Long   = rs.getLong("id")
                    val name        :String = rs.getString("name")
                    val typ         :String = rs.getString("type")
                    val frequency   :Int    = rs.getInt("frequency")

                    entities    ::= new Tuple4(id, name, frequency, typ)

                }
            }
        }

        // Get the neighborRelCount most/least relevant relations between neighbors of the node with the id "id".
        relations = List.concat(relations, getNeighborRelations(sorting, entities, neighborRelCount, id))

        val result = new JsObject(Map(("nodes", Json.toJson(entities)) , ("links", Json.toJson(relations))))

        Ok(Json.toJson(result)).as("application/json")
    }

    /**
     * Returns a list with "amount" relations between neighbors of the node with the id "id".
     */
    private def getNeighborRelations(sorting: String,
                                    entities : List[(Long, String, Int, String)],
                                    amount: Int, 
                                    id: Long) : List[(Long, Long, Long, Int)] = {

        var relations : List[(Long, Long, Long, Int)] = List()

        if(entities.size > 0){
            val sj = new StringJoiner(",", "(", ")")
            entities.filter(_._1 != id).map(node => sj.add(node._1.toString))
            val entityIdString = sj.toString()

            DB.withConnection { implicit connection =>
                val stmt = connection.createStatement
                val rs = stmt.executeQuery( "SELECT DISTINCT ON (id, frequency) id, entity1, entity2, frequency "+
                                            "FROM relationship "+
                                            "WHERE entity1 IN "+entityIdString+" "+
                                            "AND entity2 IN "+entityIdString+" "+
                                            "ORDER BY frequency "+sorting+" "+
                                            "LIMIT "+amount)
                while(rs.next()){
                    // id, source, target, frequency
                    val id          :Long   = rs.getLong("id")
                    val source      :Long   = rs.getLong("entity1")
                    val target      :Long   = rs.getLong("entity2")
                    val frequency   :Int    = rs.getInt("frequency")
                    
                    relations ::= new Tuple4(id, source, target, frequency)
                }
            }        
        }

        return relations
    }

  /**
    *
    * @param edgeId Kante
    * @return Distanz der Kante zum Fokusknoten (-1 kann nicht zurückgegeben werden)
    */
  def getDistance(edgeId: (Long, Long)): Int = {
    var queue = new mutable.Queue[(Long, Int)]();//Queue mit KnotenId und Entferung zur Ausgangskante
    //TODO Knoten die schon besucht wurden nicht nochmal suchen
    //Breitensuche nach Fokusknoten
    queue += ((edgeId._1,0))
    queue += ((edgeId._2,0))
    DB.withConnection { implicit connection =>
      val stmt = connection.createStatement
      while (queue.nonEmpty){
        val node = queue.dequeue()
        Logger.info("gD SQL: SELECT entity2 From relationship WHERE entity1 = " + node._1)
        val rs = stmt.executeQuery("SELECT entity2 From relationship WHERE entity1 = " + node._1)
        while (rs.next()) {
          if (rs.getLong("entity2") == focusId)
            return node._2 + 1
          else
            queue += ((rs.getLong("entity2"),node._2 +1 ))
        }
      }
    }
    -1
  }

/**
    *
    * @param edgeId Kante, Tuple der Form (source,target)
    * @return the computed degree of interest value of a given edge
    */
  def doI(edgeId: (Long,Long)): Double = {
    cachedDoIValues.getOrElseUpdate(edgeId,{
      val alpha = 1
      val beta = 1
      val gamma = 0
      var API : Double = 0
      var D : Double = 0
      var UI : Double = 0

      var edgeFrequency : Long = 0
      var nodeFrequency = 0


      DB.withConnection { implicit connection =>
        val stmt = connection.createStatement
        Logger.info("doI SQL: SELECT frequency From entity WHERE id = "+edgeId._1+" OR id="+edgeId._2)
        var rs = stmt.executeQuery( "SELECT frequency From entity WHERE id = "+edgeId._1+" OR id="+edgeId._2)
        var nodeFrequency = 1
        while(rs.next()){
          nodeFrequency *= rs.getInt("frequency")
        }
        Logger.info("doI SQL: SELECT frequency From relationship WHERE entity1 = "+edgeId._1+" AND entity2="+edgeId._2)
        rs = stmt.executeQuery( "SELECT frequency From relationship WHERE entity1 = "+edgeId._1+" AND entity2="+edgeId._2)
        while(rs.next()){
          edgeFrequency = rs.getInt("frequency")
        }
        val pmi = scala.math.log(edgeFrequency/nodeFrequency) //pointwise mutual information
        val npmiPlus = (pmi / -scala.math.log(edgeFrequency)+1)/2 //normalized pointwise mutual information plus

        API = npmiPlus
        D = -scala.math.pow(0.5,getDistance(edgeId)*npmiPlus)
      }

    API*alpha+D*beta+UI*gamma
    })
  }

  /**
    *
    * @param focusId anfokussierter Knoten
    * @return sendet die Kanten+Knoten an den Benutzer
    */
  def getGuidanceNodes(focusId: Long) = Action{
    Logger.info("start guidance")
    this.focusId=focusId
    cachedDoIValues.clear()
    val k = 5
    var edgeArr =new Array[(Long,Long,Long,Int)](k) //(id, source, target, frequency)
    var usedNodes = new mutable.HashSet[Long]()
    usedNodes += focusId

    var pq = mutable.PriorityQueue[(Long,Long,Long,Int)]()(Ordering.by[(Long, Long, Long, Int),Double]((x) => doI(x._2,x._3)))
    pq ++= getEdges(focusId)

    for (i <- 0 to k){//edgeArr.map ??
      val edge = pq.dequeue()
      edgeArr(i)=edge
      if ( ! usedNodes.contains(edge._2)){
        usedNodes += edge._2
        pq ++= getEdges(edge._2)
      } else {
        usedNodes += edge._3
        pq ++= getEdges(edge._3)
      }
      Logger.info(edgeArr.toString)
    }

    //bestimme Namen, Frequenz und Typ der Knoten
    val sj = new StringJoiner(",", "(", ")")
    usedNodes.foreach(node => sj.add(node.toString))
    val entityIdString = sj.toString
    val nodeArr = new Array[(Long,String,String,Int)](usedNodes.size)//(id, name, type, frequency)
    DB.withConnection { implicit connection =>
      val stmt = connection.createStatement
      Logger.info("ggN SQL: SELECT id, name, type, frequency FROM entity WHERE id IN " + entityIdString)
      val rs = stmt.executeQuery("SELECT id, name, type, frequency FROM entity WHERE id IN " + entityIdString)
      var i = 0
      while (rs.next()){
        nodeArr(i) = new Tuple4(rs.getLong("id"), rs.getString("name"), rs.getString("type"), rs.getInt("frequency"))
        i+=1
      }
    }

    val result = new JsObject(Map(("nodes", Json.toJson(nodeArr)), ("links", Json.toJson(edgeArr))))

    Ok(Json.toJson(result)).as("application/json")
  }

  /**
    *
    * @param nodeId Knoten
    * @return die an den Knoten angrenzenden Kanten in einer Liste
    */
  private def getEdges(nodeId : Long) : mutable.MutableList[(Long, Long, Long, Int)] = {
    var result = new mutable.MutableList[(Long,Long,Long,Int)]()
    DB.withConnection { implicit connection =>
      val stmt = connection.createStatement
      Logger.info("gE SQL: SELECT id,entity1,entity2,frequency From relationship WHERE entity1 = "+nodeId)
      val rs = stmt.executeQuery( "SELECT id,entity1,entity2,frequency From relationship WHERE entity1 = "+nodeId)
      while(rs.next()){
        result += ((rs.getLong("id"),rs.getLong("entity1"),rs.getLong("entity2"),rs.getInt("frequency")))
      }
      result
    }
  }


}
