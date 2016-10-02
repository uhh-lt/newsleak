// scalastyle:off
package controllers.network

import model.faceted.search.{ FacetedSearch, Facets, NodeBucket }
import play.api.Logger
import play.api.libs.json
import play.api.libs.json.{ JsObject, JsValue, Json, Writes }
import scalikejdbc._

import scala.math.Ordering
import scala.util.control._
import util.TupleWriters._

import scala.collection.mutable

/**
 * Created by martin on 11.09.16.
 */
class NSession {
  implicit val session = AutoSession
  val typeIndex = collection.immutable.HashMap("PER" -> 0, "ORG" -> 1, "LOC" -> 2, "MISC" -> 3)
  //val EpN = 4 //Kanten pro Knoten
  val newEdgesPerIter = 100

  /**
   * Gibt an wie viele Iterationen schon vollzogen wurdn
   */
  var iter: Int = 0

  var nodes = new mutable.HashMap[Long, Node]()
  var edges = new mutable.HashMap[(Node, Node), Edge]()
  var oldEdges = new mutable.HashMap[(Node, Node), Edge]()

  /**
   * Map cacht Knoten deren DoI-Wert schonmal berechnet wurden. Wird mit dem Beginn jedes Guidance-Schrittes zurückgesetzt.
   */
  var uiMatrix = Array.ofDim[Int](4, 4)

  /**
   *
   * @param focusId anfokussierter Knoten
   * @return sendet die Kanten+Knoten an den Benutzer
   */
  def getGuidanceNodes(focusId: Long, edgeAmount: Int, epn: Int, uiMatrix: Array[Array[Int]], useOldEdges: Boolean): json.JsValue = {
    iter += 1
    //nodes.clear
    edges.clear
    if (!useOldEdges) {
      oldEdges.clear
    }

    //TODO Node Extraktion in Funktion auslagern
    var startNode = sql"""SELECT id, name, type, dococc FROM entity_ext WHERE id = $focusId"""
      .map(rs => (rs.long("id"), rs.string("name"), rs.int("dococc"), rs.string("type"))).first().apply().map(t => new Node(t._1, t._2, t._3, 0, t._4, iter)).get
    startNode = nodes.getOrElseUpdate(focusId, startNode)
    startNode.update(0, iter)

    Logger.info("start guidance")
    //Logger.info(uiMatrix.deep.mkString("\n"))
    this.uiMatrix = uiMatrix

    var usedEdges = new mutable.HashMap[(Node, Node), Edge]()
    var usedNodes = new mutable.HashSet[Node]()
    usedNodes += startNode
    //nodes += ((focusId, startNode))

    var pq = mutable.PriorityQueue[Edge]()(Ordering.by[Edge, Double](_.getDoi))
    pq ++= getEdges(startNode)
    //Logger.info(pq.toString())

    val loop = new Breaks
    loop.breakable {
      for (i <- 0 until edgeAmount) {
        //edgeArr.map ??
        object pqIter extends Iterator[Edge] { // ACHTUNG pq gibt mit .find() nicht nach DoI geordnet aus! Deswegen ist das hier notwendig
          def hasNext = pq.nonEmpty
          def next = pq.dequeue
        }
        val edgeO = pqIter.find(e => !{
          Logger.info("" + e.toString(true) + "E1:" + e.getNodes._1.getConn + "E2:" + e.getNodes._2.getConn)
          usedEdges.contains(e.getNodes) || usedEdges.contains(e.getNodes.swap) || e.getNodes._1.getConn == epn || e.getNodes._2.getConn == epn
        })

        val edge = edgeO.getOrElse({
          Logger.warn("Es wurden weniger Kanten als geplant ausgegeben da der Priority Queue leer ist. EpN hochsetzen?")
          loop.break()
        })

        edge.getNodes._1.incConn()
        edge.getNodes._2.incConn()

        usedEdges += ((edge.getNodes, edge))

        Logger.info("" + usedEdges)
        Logger.info("added: " + edge.toString(true))

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
        //Logger.info(edgeMap.toString)
      }
    }

    val result = new JsObject(Map(("nodes", Json.toJson(usedNodes)), ("links", Json.toJson(usedEdges.values))))

    // Json.toJson(usedEdges.values.map(e => Map(("id", 0), ("sourceNode", e.getNodes._1.getId), ("targetNode", e.getNodes._2.getId), ("docOcc", e.getDocOcc), ("specialWeight", e.hasSpecialWeight)))))))
    // Json.toJson(usedEdges.values.map(e => (0, e.getNodes._1.getId, e.getNodes._2.getId, e.getDocOcc, e.hasSpecialWeight))))))
    Logger.info("" + usedEdges.size)
    oldEdges = usedEdges
    Json.toJson(result)
  }

  implicit val NodeWrite = new Writes[Node] {
    override def writes(n: Node) = Json.obj(
      "id" -> n.getId,
      "name" -> n.getName,
      "docOcc" -> n.getDocOcc,
      "type" -> n.getCategory,
      "edges" -> n.getRelevantEdges.map(_.getNodes._2.getName)
    )
  }

  implicit val EdgeWrite = new Writes[Edge] {
    def writes(e: Edge) = Json.obj(
      "id" -> 0,
      "sourceNode" -> e.getNodes._1.getId,
      "targetNode" -> e.getNodes._2.getId,
      "docOcc" -> e.getDocOcc,
      "uiLevel" -> e.getUiLevel
    )
  }

  /**
   *
   * @param node Knoten
   * @return die an den Knoten angrenzenden Kanten in einer Liste
   */
  private def getEdges(node: Node): List[Edge] = {

    var distToFocus: Int = node.getDistance //Wenn der Knoten nicht in der Map liegt, muss es sich um dem Fokus handeln, also dist=0
    distToFocus += 1
    val nodeBuckets = FacetedSearch.aggregateEntities(Facets(List(), Map(), List(node.getId), None, None), newEdgesPerIter, List(), 1).buckets
    val edgeFreqTuple = nodeBuckets.collect { case NodeBucket(id, docOccurrence) => (id, docOccurrence.toInt) }.filter(_._1 != node.getId)
    val nodeMap: mutable.HashMap[Long, Node] = new mutable.HashMap[Long, Node]()
    nodeMap ++= sql"""SELECT id, name, type, dococc FROM entity_ext WHERE id IN (${edgeFreqTuple.map(_._1)}) AND dococc IS NOT NULL"""
      .map(rs => (rs.long(1), rs.string(2), rs.string(3), rs.int(4))).list.apply.map(x => (x._1, new Node(x._1, x._2, x._4, distToFocus, x._3, iter)))

    var newEdges = new mutable.HashMap[(Node, Node), Edge]()
    edgeFreqTuple.foreach(et => {
      if (nodeMap.contains(et._1)) {
        var n = nodeMap(et._1)
        n = nodes.getOrElseUpdate(n.getId, n)
        n.update(distToFocus, iter)
        var oldDoi = 0.0
        if (oldEdges.contains((node, n))) {
          oldDoi = oldEdges((node, n)).getDoi
        } else if (oldEdges.contains((n, node))) {
          oldDoi = oldEdges((n, node)).getDoi
        }
        newEdges += (((node, n), new Edge(node, n, et._2, uiMatrix(typeIndex(node.getCategory))(typeIndex(n.getCategory)), oldDoi)))
      }
    })

    //TODO Distanzen müssen rekursiv geupdatet werden

    newEdges.values.toList
  }

}

