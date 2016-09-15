// scalastyle:off
package controllers.network

import play.api.Logger
import play.api.libs.json
import play.api.libs.json.{JsObject, Json}
import scalikejdbc._

import scala.math.Ordering
// to read files

import util.TupleWriters._

import scala.collection.mutable
import scala.math._

/**
 * Created by martin on 11.09.16.
 */
class NSession {
  implicit val session = AutoSession
  val typeIndex = collection.immutable.HashMap("PER" -> 0, "ORG" -> 1, "LOC" -> 2, "MISC" -> 3)

  /**
   * Knoten der gerade anfokussiert wird
   */
  var focusId: Long = -1

  /**
   * Map cacht Knoten deren DoI-Wert schonmal berechnet wurden. Wird mit dem Beginn jedes Guidance-Schrittes zurückgesetzt.
   */
  var cachedDoIValues: mutable.HashMap[(Long, Long), Double] = new mutable.HashMap[(Long, Long), Double]()

  var cachedEdgeFreq: mutable.HashMap[(Long, Long), Int] = new mutable.HashMap[(Long, Long), Int]()

  var cachedEdgeId: mutable.HashMap[(Long, Long), Long] = new mutable.HashMap[(Long, Long), Long]()

  var cachedDistanceValues: mutable.HashMap[Long, Int] = new mutable.HashMap[Long, Int]()

  var uiMatrix = Array.ofDim[Double](4, 4)

  /**
   *
   * @param edgeId Kante, Tuple der Form (source,target)
   * @return the computed degree of interest value of a given edge
   */
  def doI(edgeId: (Long, Long)): Double = {
    cachedDoIValues(edgeId)
  }

  /**
   *
   * @param focusId anfokussierter Knoten
   * @return sendet die Kanten+Knoten an den Benutzer
   */
  def getGuidanceNodes(focusId: Long, edgeAmount: Int, uiMatrix: Array[Array[Double]]): json.JsValue = {

    Logger.info("start guidance")
    Logger.info(uiMatrix.deep.mkString("\n"))
    this.focusId = focusId
    this.uiMatrix = uiMatrix
    cachedDoIValues.clear()
    cachedDistanceValues.clear()

    var edgeSet = new mutable.HashSet[(Long, Long)]() //(id, source, target, frequency)
    var usedNodes = new mutable.HashSet[Long]()
    usedNodes += focusId

    var pq = mutable.PriorityQueue[(Long, Long)]()(Ordering.by[(Long, Long), Double]((x) => doI(x._1, x._2)))
    pq ++= getEdges(focusId)

    for (i <- 0 until edgeAmount) { //edgeArr.map ??

      var edge = pq.dequeue()
      while (edgeSet.contains(edge)) { //therotisch kann eine Kante bis zu 2x aus pq entnommen werden. Wenn sie einmal von jedem Knoten ausgehend hinzugefügt wurde.
        edge = pq.dequeue()
      }

      Logger.info("E:" + edge._1 + "," + edge._2 + " V:" + doI(edge) + "freq:" + cachedEdgeFreq(edge))

      edgeSet += edge

      if (!usedNodes.contains(edge._2)) {
        usedNodes += edge._2
        //if (i < k / 2) { //nur für die ersten k/2 Kanten werden weitere Kanten gesucht
        pq ++= getEdges(edge._2)
        //}
      } else if (!usedNodes.contains(edge._1)) {
        usedNodes += edge._1
        //if (i < k / 2) { //nur für die ersten k/2 Kanten werden weitere Kanten gesucht
        pq ++= getEdges(edge._1)
        //}
      }
      Logger.info(edgeSet.toString)
    }

    //bestimme Namen, Frequenz und Typ der Knoten
    Logger.info("ggN SQL: SELECT id, name, type, frequency FROM entity WHERE id IN ")
    val nodes = sql"""SELECT id, name, type, frequency FROM entity WHERE id IN ($usedNodes)"""
      .map(rs => (rs.long("id"), rs.string("name"), rs.int("frequency"), rs.string("type"))).list().apply()

    val result = new JsObject(Map(("nodes", Json.toJson(nodes)), ("links", Json.toJson(edgeSet.map(e => (cachedEdgeId.apply(e), e._1, e._2, cachedEdgeFreq.apply(e))))))) //TODO Kantenattribute müssen auch zurückgegeben werden

    Json.toJson(result)
  }

  /**
   *
   * @param nodeId Knoten
   * @return die an den Knoten angrenzenden Kanten in einer Liste
   */
  private def getEdges(nodeId: Long): List[(Long, Long)] = {

    var distToFocus = cachedDistanceValues.getOrElse(nodeId, -1) //Wenn der Knoten nicht in der Map liegt, muss es sich um dem Fokus handeln, also dist=0
    distToFocus += 1
    val query1 =
      sql"""SELECT entity1, entity2, relationship.frequency AS efreq, et1.frequency AS n1freq, et2.frequency AS n2freq, relationship.id, et1.type, et2.type
          FROM relationship LEFT JOIN entity AS et1 ON et1.id = relationship.entity1
          LEFT JOIN entity AS et2 ON et2.id = relationship.entity2 WHERE entity1 = $nodeId ORDER BY efreq DESC LIMIT 100"""
    val query2 =
      sql"""SELECT entity1, entity2, relationship.frequency AS efreq, et1.frequency AS n1freq, et2.frequency AS n2freq, relationship.id, et1.type, et2.type
          FROM relationship LEFT JOIN entity AS et1 ON et1.id = relationship.entity1
          LEFT JOIN entity AS et2 ON et2.id = relationship.entity2 WHERE entity2 = $nodeId ORDER BY efreq DESC LIMIT 100""" //zwei seperate SQL Abfragen sind schnelle als eine per union vereinte Abfrage
    def useResult(sqlresult: SQL[Nothing, NoExtractor]): List[(Long, Long)] =
      {

        val result = sqlresult.map(rs => {
          (rs.long(1), rs.long(2), rs.int(3), rs.int(4), rs.int(5), rs.long(6), rs.string(7), rs.string(8)) //TODO Limit ist veränderbar
        })
          .toList().apply() //.filter(x => cachedDistanceValues.getOrElse(x._2, distToFocus + 1) > distToFocus && x._3 > 0)
          .map(x => ((x._1, x._2), 1 / -log((x._3: Double) * (x._3: Double) / ((x._4: Double) * (x._5: Double))), x._3, x._6, x._7, x._8))

        val use2ndNode = if (result.nonEmpty) nodeId == result.head._1._1 else false //Wenn nodeId der erste Knoten im Kantentupel ist, müssen wir die Distanzen der zweiten Knoten überprüfen

        result.foreach(x =>
          if (cachedDistanceValues.getOrElseUpdate(if (use2ndNode) x._1._2 else x._1._1, distToFocus) > distToFocus) {
            cachedDistanceValues(if (use2ndNode) x._1._2 else x._1._1) = distToFocus
            //TODO Distanzen müssen rekursiv geupdatet werden
          })

        cachedEdgeFreq ++= result.map(x => (x._1, x._3))
        cachedEdgeId ++= result.map(x => (x._1, x._4))
        cachedDoIValues ++= result.map(x => (x._1, {
          if (uiMatrix(typeIndex(x._5))(typeIndex(x._6)) == Double.NegativeInfinity) {
            Double.NegativeInfinity //Wenn Kanten dieses Types nicht berücksichtig werden sollen, setze den DoI Wert auf -inf
          } else {
            val alpha = 1
            val beta = 0
            val gamma = 1

            val API = x._2
            val D = 0 //-(1 - scala.math.pow(0.5, distToFocus) * (x._2))

            val UI = x._2 * uiMatrix(typeIndex(x._5))(typeIndex(x._6))
            API * alpha + beta * D + UI * gamma
          }
        }))

        result.map(_._1)
      }

    useResult(query1) ++ useResult(query2)
  }

}

