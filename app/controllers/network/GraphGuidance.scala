/*

 */
package controllers.network

import java.util.NoSuchElementException

import model.faceted.search.{ FacetedSearch, Facets, NodeBucket }
import play.api.Logger
import util.SessionUtils.currentDataset

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.math.Ordering

import model.faceted.search.{ FacetedSearch, Facets, NodeBucket }
import model.{ Entity, EntityType }
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.{ Action, Controller, Results }
import util.TimeRangeParser
import util.SessionUtils.currentDataset

/**
 * Created by martin on 02.10.16.
 */

class GraphGuidance() { outer =>
  implicit val context = this

  val typeIndex = collection.immutable.HashMap("PER" -> 0, "ORG" -> 1, "LOC" -> 2, "MISC" -> 3)
  val newEdgesPerIter = 100

  // Gibt an wie viele Iterationen schon vollzogen wurdn
  var iter: Int = 0

  // cacht schon besuchte Knoten
  var nodes = new mutable.HashMap[Long, Node]() // immer gültig
  var oldEdges = new mutable.HashMap[(Long, Long), Edge]() // wird beibehalten wenn useOldEdges, sonst nicht

  var usedNodes: mutable.HashMap[Long, Node] = _ // wird mit jedem Guidance-Schritt neugeneriert

  var facets = Facets.empty
  var uiMatrix = Array.ofDim[Int](4, 4)
  var epn = 4
  var edgeAmount = 20
  var focusNodeId: Long = _

  implicit val order = Ordering.by[Edge, (Double, Long, Long)](e => (
    e.getDoi, e.getNodes._1.getId, e.getNodes._2.getId
  ))

  // def getGuidance mit GuidanceInternals
  // def init erzeugt eigene Internals aus normalen Parametern
  // normales getGuidance ruft init auf, mit den erstellten Parametern dann das andere getGuidance

  // scalastyle:off
  def createIterator: GuidanceIterator = {

    new GuidanceIterator {
      var nodeList: mutable.HashMap[Long, mutable.PriorityQueue[Edge]] = _ // wird mit jedem Guidance-Schritt neugeneriert
      var usedEdges: mutable.HashMap[(Long, Long), Edge] = _
      var pq: mutable.PriorityQueue[Edge] = _

      override def init(facets: Facets, focusId: Long, edgeAmount: Int, epn: Int, uiMatrix: Array[Array[Int]], useOldEdges: Boolean): GuidanceIterator = {
        outer.facets = facets
        outer.epn = epn
        outer.uiMatrix = uiMatrix
        outer.edgeAmount = edgeAmount
        outer.focusNodeId = focusId

        iter += 1

        if (!useOldEdges) {
          oldEdges.clear
        }

        val startNode = nodes.getOrElseUpdate(focusId, NodeFactory.createNodes(facets, List(focusId), 0, iter).head)
        startNode.update(facets, 0, iter)

        //Logger.trace("start guidance")

        usedEdges = new mutable.HashMap[(Long, Long), Edge]()
        nodeList = new mutable.HashMap[Long, mutable.PriorityQueue[Edge]]()
        usedNodes = new mutable.HashMap[(Long), Node]()
        // usedNodes.clear()
        usedNodes += (startNode.getId -> startNode)

        pq = mutable.PriorityQueue[Edge]() ++= getEdges(startNode)

        this
      }

      override def initWithState(state: GuidanceIteratorState): GuidanceIterator = {
        nodeList = state.nodeList
        pq = state.pq
        usedEdges = state.usedEdges
        usedNodes = state.usedNodes

        facets = state.facets
        epn = state.epn
        uiMatrix = state.uiMatrix
        edgeAmount = state.edgeAmount
        if (!state.useOldEdges) {
          oldEdges.clear
        }
        this
      }

      override def getState: GuidanceIteratorState =
        new GuidanceIteratorState(
          nodeList,
          pq,
          usedEdges,
          usedNodes,
          facets,
          edgeAmount,
          epn,
          uiMatrix,
          true
        )

      override def hasNext: Boolean = {
        pq.exists(e => !(usedEdges.contains(e.getNodesId) || usedEdges.contains(e.getNodesId.swap) || e.getNodes._1.getConn == epn || e.getNodes._2.getConn == epn))
      }

      override def next(): (Edge, Option[Node]) = {

        object pqIter extends Iterator[Edge] {
          // ACHTUNG pq gibt mit .find() nicht nach DoI geordnet aus! Deswegen ist das hier notwendig
          def hasNext = pq.nonEmpty
          def next = pq.dequeue
        }
        val edgeO = pqIter.find(e => !{
          usedEdges.contains(e.getNodesId) || usedEdges.contains(e.getNodesId.swap) || e.getNodes._1.getConn == epn || e.getNodes._2.getConn == epn
        })

        val edge = edgeO.getOrElse({
          throw new NoSuchElementException
        })

        edge.getNodes._1.incConn()
        edge.getNodes._2.incConn()

        usedEdges += ((edge.getNodesId, edge))

        Logger.trace("" + usedEdges)
        Logger.trace("added: " + edge.toString(true))

        var newNode: Option[Node] = None
        if (!usedNodes.contains(edge.getNodes._2.getId)) {
          newNode = Some(edge.getNodes._2)
          //Logger.trace(newNode.get.toString)
          usedNodes += (edge.getNodes._2.getId -> edge.getNodes._2)
          //if (i < k / 2) { //nur für die ersten k/2 Kanten werden weitere Kanten gesucht
          pq ++= getEdges(edge.getNodes._2)
          //}
        }
        oldEdges += (edge.getNodes._1.getId, edge.getNodes._2.getId) -> edge
        (edge, newNode)
      }

      override def getMoreEdges(nodeId: Long, amount: Int): (List[Edge], List[Node]) = {
        var pq = nodeList(nodeId)
        object pqIter extends Iterator[(Edge, Option[Node])] {
          // ACHTUNG pq gibt mit .find() nicht nach DoI geordnet aus! Deswegen ist das hier notwendig
          def hasNext = pq.nonEmpty
          def next = {
            val edge = pq.dequeue
            var node: Option[Node] = None
            if (nodes.contains(edge.getNodes._2.getId)) {
              node = Some(edge.getNodes._2)
            }
            oldEdges += (edge.getNodes._1.getId, edge.getNodes._2.getId) -> edge
            (edge, node)
          }
        }
        // Logger.debug(usedEdges.keySet.toString())
        val (eList, nList) = pqIter.filterNot(tpl => usedEdges.contains(tpl._1.getNodesId) || usedEdges.contains(tpl._1.getNodesId.swap)).take(amount).map(tpl => {
          usedEdges += tpl._1.getNodesId -> tpl._1
          if (tpl._2.nonEmpty) {
            usedNodes += tpl._2.get.getId -> tpl._2.get
            if (!nodeList.contains(tpl._2.get.getId)) {
              getEdges(tpl._2.get)
            }
          }
          tpl
        }).toList.unzip
        (eList, nList.flatten)
      }

      /**
       * @param amount Anzahl der Knoten im Iterator
       * @return Gibt einen Iterator mit Knoten zurück, die momentan nicht im generierten Subgraph vorhanden sind,
       * aber vorhanden wären, wenn von diesem Knoten aus eine Guidance gestartet würde
       */
      override def getGuidancePreview(nodeId: Long, amount: Int): List[Node] = {
        val ggIter = getCopyGuidance(nodeId, true)
        // val ggIter = gg.getGuidance(id, context.edgeAmount, context.epn, context.uiMatrix, false, List())
        // Logger.debug(ggIter.map(_._2.get).toString())

        ggIter.take(edgeAmount).filterNot(t => {
          t._2.isEmpty || usedNodes.contains(t._2.get.getId)
        }).take(amount).map(_._2.get).toList
      }

      override def getConnectionsByType(nodeId: Long): List[(String, Long)] = {
        nodes(nodeId).getConnectionsByType
      }

      private def getEdges(node: Node): mutable.MutableList[Edge] = {

        var distToFocus: Int = node.getDistance //Wenn der Knoten nicht in der Map liegt, muss es sich um dem Fokus handeln, also dist=0
        distToFocus += 1
        val nodeBuckets = FacetedSearch.fromIndexName("cable").aggregateEntities(facets.withEntities(List(node.getId)), newEdgesPerIter, List(), Nil, 1).buckets
        val edgeFreqTuple = nodeBuckets.collect { case NodeBucket(id, docOccurrence) => (id, docOccurrence.toInt) }.filter(_._1 != node.getId)
        val nodeMap = NodeFactory.createNodes(facets, edgeFreqTuple.map(_._1), distToFocus, iter).map(n => n.getId -> n).toMap

        var newEdges = new mutable.MutableList[Edge]()
        edgeFreqTuple.foreach(et => {
          if (nodeMap.contains(et._1)) {
            var n = nodeMap(et._1)
            n = nodes.getOrElseUpdate(n.getId, n)
            n.update(facets, distToFocus, iter)
            var oldDoi = 0.0
            if (oldEdges.contains((node.getId, n.getId))) {
              oldDoi = oldEdges((node.getId, n.getId)).getDoi
            } else if (oldEdges.contains((n.getId, node.getId))) {
              oldDoi = oldEdges((n.getId, node.getId)).getDoi
            }
            newEdges += new Edge(node, n, et._2, uiMatrix(typeIndex(node.getCategory))(typeIndex(n.getCategory)), oldDoi)
          }
        })
        nodeList += (node.getId -> (new mutable.PriorityQueue[Edge]() ++= newEdges))
        //TODO Distanzen müssen rekursiv geupdatet werden
        newEdges
      }
    }
  }

  def copy: GraphGuidance = {
    val newGG = new GraphGuidance
    newGG.oldEdges = oldEdges.map(p => p._1 -> p._2.copy)
    newGG.nodes = nodes.map(p => p._1 -> p._2.copy)
    newGG.iter = iter

    newGG.facets = facets //new
    newGG.uiMatrix = uiMatrix
    newGG.focusNodeId = focusNodeId
    newGG.epn = epn
    newGG.edgeAmount = edgeAmount
    newGG
  }

  def getCopyGuidance(focusId: Long, useOldEdges: Boolean): Iterator[(Edge, Option[Node])] = {
    this.copy.createIterator.init(facets, focusId, edgeAmount, epn, uiMatrix, useOldEdges)
  }
}
