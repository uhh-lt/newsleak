// scalastyle:off
package controllers.network

import model.faceted.search.{ FacetedSearch, Facets, NodeBucket }
import play.api.Logger
import play.api.libs.json
import play.api.libs.json.{ JsObject, Json }
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
    val nodeTuple = sql"""SELECT id, name, type, dococc FROM entity_ext WHERE id = $focusId"""
      .map(rs => (rs.long("id"), rs.string("name"), rs.int("dococc"), rs.string("type"))).list().apply().head
    val startNode = new Node(nodeTuple._1, nodeTuple._2, nodeTuple._3, 0, nodeTuple._4, iter)
    Logger.info("start guidance")
    Logger.info(uiMatrix.deep.mkString("\n"))
    this.uiMatrix = uiMatrix

    var edgeMap = new mutable.HashMap[(Node, Node), Edge]() //(id, source, target, frequency)
    var usedNodes = new mutable.HashSet[Node]()
    usedNodes += startNode
    nodes += ((focusId, startNode))

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
    val result = new JsObject(Map(("nodes", Json.toJson(usedNodes.map(n => { (n.getId, n.getName, n.getDocOcc, n.getCategory) }))), ("links", Json.toJson(edgeMap.values.map(e => (0, e.getNodes._1.getId, e.getNodes._2.getId, e.getDocOcc))))))
    Logger.info("" + edgeMap.size)
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
    val nodeBuckets = FacetedSearch.aggregateEntities(Facets(List(), Map(), List(node.getId), None, None), 100, List(), 1).buckets
    val edgeFreqTuple = nodeBuckets.collect { case NodeBucket(id, docOccurrence) => (id, docOccurrence.toInt) }.filter(_._1 != node.getId)
    val nodeMap: mutable.HashMap[Long, Node] = new mutable.HashMap[Long, Node]()
    nodeMap ++= sql"""SELECT id, name, type, dococc FROM entity_ext WHERE id IN (${edgeFreqTuple.map(_._1)})"""
      .map(rs => (rs.long(1), rs.string(2), rs.string(3), rs.int(4))).list.apply.map(x => (x._1, new Node(x._1, x._2, x._4, distToFocus, x._3, iter)))

    var newEdges = new mutable.HashMap[(Node, Node), Edge]()
    edgeFreqTuple.foreach(et => {
      var n = nodeMap(et._1)
      n = nodes.getOrElseUpdate(n.getId, n)
      n.update(distToFocus, iter)
      newEdges += (((node, n), new Edge(node, n, et._2, uiMatrix(typeIndex(node.getCategory))(typeIndex(n.getCategory)))))
    })

    //TODO Distanzen müssen rekursiv geupdatet werden

    newEdges.values.toList
  }

}

