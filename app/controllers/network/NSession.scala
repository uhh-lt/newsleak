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

/**
 * Created by martin on 11.09.16.
 */
class NSession {
  implicit val session = AutoSession
  val typeIndex = collection.immutable.HashMap("PER" -> 0, "ORG" -> 1, "LOC" -> 2, "MISC" -> 3)

  /**
   * Gibt an wie viele Iterationen schon vollzogen wurdn
   */
  var iter: Int = 0
  /**
   * Knoten der gerade anfokussiert wird
   */

  var nodes = new mutable.HashMap[Long, Node]()
  var edges = new mutable.HashMap[(Node, Node), Edge]()

  /**
   * Map cacht Knoten deren DoI-Wert schonmal berechnet wurden. Wird mit dem Beginn jedes Guidance-Schrittes zurückgesetzt.
   */
  var uiMatrix = Array.ofDim[Double](4, 4)

  /**
   *
   * @param focusId anfokussierter Knoten
   * @return sendet die Kanten+Knoten an den Benutzer
   */
  def getGuidanceNodes(focusId: Long, edgeAmount: Int, uiMatrix: Array[Array[Double]]): json.JsValue = {
    iter += 1
    edges.clear
    nodes.clear
    val nodeTuple = sql"""SELECT id, name, type, frequency FROM entity WHERE id = $focusId"""
      .map(rs => (rs.long("id"), rs.string("name"), rs.int("frequency"), rs.string("type"))).list().apply().head
    val startNode = new Node(nodeTuple._1, nodeTuple._3, 0, nodeTuple._4, iter)
    Logger.info("start guidance")
    Logger.info(uiMatrix.deep.mkString("\n"))
    this.uiMatrix = uiMatrix

    var edgeMap = new mutable.HashMap[(Node, Node), Edge]() //(id, source, target, frequency)
    var usedNodes = new mutable.HashSet[Node]()
    usedNodes += startNode

    var pq = mutable.PriorityQueue[Edge]()(Ordering.by[Edge, Double](_.getDoi))
    pq ++= getEdges(startNode)
    Logger.info(pq.toString())
    for (i <- 0 until edgeAmount) { //edgeArr.map ??

      var edge = pq.dequeue()
      while (edgeMap.contains(edge.getNodes) || edgeMap.contains((edge.getNodes._2, edge.getNodes._1))) { //therotisch kann eine Kante bis zu 2x aus pq entnommen werden. Wenn sie einmal von jedem Knoten ausgehend hinzugefügt wurde.
        edge = pq.dequeue()
        Logger.info(edge.toString)
      }

      Logger.info(edge.toString)

      edgeMap += ((edge.getNodes, edge))

      if (!usedNodes.contains(edge.getNodes._2)) {
        usedNodes += edge.getNodes._2
        //if (i < k / 2) { //nur für die ersten k/2 Kanten werden weitere Kanten gesucht
        pq ++= getEdges(edge.getNodes._2)
        //}
      } else if (!usedNodes.contains(edge.getNodes._1)) {
        usedNodes += edge.getNodes._1
        //if (i < k / 2) { //nur für die ersten k/2 Kanten werden weitere Kanten gesucht
        pq ++= getEdges(edge.getNodes._1)
        //}
      }
      Logger.info(edgeMap.toString)
    }

    //bestimme Namen, Frequenz und Typ der Knoten
    Logger.info("ggN SQL: SELECT id, name, type, frequency FROM entity WHERE id IN ")
    val nodesTuple = sql"""SELECT id, name, type, frequency FROM entity WHERE id IN (${usedNodes.map(_.getId)})"""
      .map(rs => (rs.long("id"), rs.string("name"), rs.int("frequency"), rs.string("type"))).list().apply()
    Logger.info(nodesTuple.toString)
    val result = new JsObject(Map(("nodes", Json.toJson(nodesTuple)), ("links", Json.toJson(edgeMap.values.map(e => (0, e.getNodes._1.getId, e.getNodes._2.getId, e.getDocOcc)))))) //TODO Kantenattribute müssen auch zurückgegeben werden

    Json.toJson(result)
  }

  /**
   *
   * @param node Knoten
   * @return die an den Knoten angrenzenden Kanten in einer Liste
   */
  private def getEdges(node: Node): List[Edge] = {

    var distToFocus: Int = node.getDistance //Wenn der Knoten nicht in der Map liegt, muss es sich um dem Fokus handeln, also dist=0
    distToFocus += 1
    val query1 =
      sql"""SELECT entity1, entity2, relationship.frequency AS efreq, et1.frequency AS n1freq, et2.frequency AS n2freq, relationship.id, et1.type, et2.type
          FROM relationship LEFT JOIN entity AS et1 ON et1.id = relationship.entity1
          LEFT JOIN entity AS et2 ON et2.id = relationship.entity2 WHERE entity1 = ${node.getId} ORDER BY efreq DESC LIMIT 100"""
    val query2 =
      sql"""SELECT entity1, entity2, relationship.frequency AS efreq, et1.frequency AS n1freq, et2.frequency AS n2freq, relationship.id, et1.type, et2.type
          FROM relationship LEFT JOIN entity AS et1 ON et1.id = relationship.entity1
          LEFT JOIN entity AS et2 ON et2.id = relationship.entity2 WHERE entity2 = ${node.getId} ORDER BY efreq DESC LIMIT 100""" //zwei seperate SQL Abfragen sind schnelle als eine per union vereinte Abfrage
    def useResult(sqlresult: SQL[Nothing, NoExtractor]): mutable.HashMap[(Node, Node), Edge] =
      {
        var newEdges = new mutable.HashMap[(Node, Node), Edge]()
        val result = sqlresult.map(rs => {
          //e1.id,e2.id,r.freq,e1.freq,e2.freq,r.id,e1.type,e2.type
          (rs.long(1), rs.long(2), rs.int(3), rs.int(4), rs.int(5), rs.long(6), rs.string(7), rs.string(8)) //TODO Limit ist veränderbar
        })
          .toList().apply()
        val use2ndNode = if (result.nonEmpty) node.getId == result.head._1 else false //Wenn nodeId der erste Knoten im Kantentupel ist, müssen wir die Distanzen der zweiten Knoten überprüfen

        result.foreach(r => {
          var n = new Node(if (!use2ndNode) r._1 else r._2, if (!use2ndNode) r._4 else r._5, distToFocus, if (!use2ndNode) r._7 else r._8, iter)
          n = nodes.getOrElseUpdate(n.getId, n)
          n.update(distToFocus,iter)
          newEdges += (((node, n), new Edge(node, n, r._3, uiMatrix(typeIndex(r._7))(typeIndex(r._8)))))
        })
        //.filter(x => cachedDistanceValues.getOrElse(x._2, distToFocus + 1) > distToFocus && x._3 > 0)

        //TODO Distanzen müssen rekursiv geupdatet werden

        newEdges
      }

    (useResult(query1).values ++ useResult(query2).values).toList
  }

}

