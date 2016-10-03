/*
 *
 */
package controllers.network

import model.faceted.search.{ FacetedSearch, Facets, NodeBucket }
import play.api.Logger
import scalikejdbc._

import scala.collection.mutable
import scala.math.Ordering

/**
 * Created by martin on 17.09.16.
 */

object NodeFactory {
  implicit val session = AutoSession

  /**
   * @param nodeIds Liste von Entitaeten-Ids
   * @param distToFocus Distanz zum Fokus-Knoten
   * @param iter gibt an wie viele Guidance Schritte schon absolviert wurden
   * @return Liste von Nodes
   */
  def createNodes(nodeIds: List[Long], distToFocus: Int, iter: Int)(implicit context: GraphGuidance): List[Node] = {
    sql"""SELECT id, name, type, dococc FROM entity_ext WHERE id IN (${nodeIds}) AND dococc IS NOT NULL"""
      .map(rs => (new Node(rs.long(1), rs.string(2), rs.int(4), distToFocus, rs.string(3), iter))).list.apply
  }
}

class Node(id: Long, name: String, docOcc: Int, var distance: Int, category: String, var iter: Int)(implicit context: GraphGuidance) {
  implicit val session = AutoSession

  val numberOfRelEdges = 4
  val newEdgesPerIter = 100

  var connectedEdges = 0

  lazy val relevantNodes = {
    val ggIter = context.getCopyGuidance(id, true)
    // val ggIter = gg.getGuidance(id, context.edgeAmount, context.epn, context.uiMatrix, false, List())
    ggIter.take(context.edgeAmount).filter(t =>
      !(t._2.isEmpty || context.usedNodes.contains(t._2.get.getId))
    // !(context.edges.contains(t._1.getNodes) || context.edges.contains(t._1.getNodes.swap))
    ).take(numberOfRelEdges).map(_._2.get)

    /*
    var pq = mutable.PriorityQueue[Edge]()(Ordering.by[Edge, Double](_.getDoi))
    val nodeBuckets = FacetedSearch.aggregateEntities(Facets(List(), Map(), List(id), None, None), newEdgesPerIter, List(), 1).buckets
    val edgeFreqTuple: Map[Long, Int] = nodeBuckets.collect { case NodeBucket(nid, docOccurrence) => (nid.toLong, docOccurrence.toInt) }.filter(_._1 != id)
      .map(et => et._1 -> et._2)(collection.breakOut)
    val nodeMap: mutable.HashMap[Long, Node] = new mutable.HashMap[Long, Node]()
    nodeMap ++=
      sql"""SELECT id, name, type, dococc FROM entity_ext WHERE id IN (${edgeFreqTuple.map(_._1)}) AND dococc IS NOT NULL"""
      // TODO in eigene Funktion verschieben
      .map(rs => (rs.long(1), rs.string(2), rs.string(3), rs.int(4))).list.apply.map(x => (x._1, new Node(x._1, x._2, x._4, 1, x._3, iter)))

    pq ++= nodeMap.values.map(n => {
      new Edge(this, n, edgeFreqTuple(n.getId), /* uiMatrix(typeIndex(node.getCategory))(typeIndex(n.getCategory)) */ 1, 0)
    })

    object pqIter extends Iterator[Edge] {
      // ACHTUNG pq gibt mit .find() nicht nach DoI geordnet aus! Deswegen ist das hier notwendig
      def hasNext = pq.nonEmpty
      def next = {
        val x = pq.dequeue
        Logger.info("" + x)
        x
      }
    }

    pqIter.take(numberOfRelEdges).toList
    */
  }

  def getId: Long = {
    id
  }

  def getName: String = {
    name
  }

  def getDocOcc: Int = {
    docOcc
  }

  def getCategory: String = {
    category
  }

  def getDistance: Int = {
    distance
  }

  def incConn() = {
    connectedEdges += 1
  }

  def getConn: Int = {
    connectedEdges
  }

  def update(distance: Int, iter: Int) = {
    if (iter == this.iter) {
      if (distance < this.distance) {
        // TODO Kanten müssen rekursiv geupdatet werden
        this.distance = distance
      }
    } else {
      this.iter = iter
      connectedEdges = 0
      this.distance = distance
    }
  }

  override def toString: String = {
    "(Name: " + name + ")"
  }

  def getRelevantNodes: Iterator[Node] = {
    relevantNodes
  }

  /*
  // scalastyle:off
  def ==(that: Node): Boolean = {
    id == that.getId
  }

  def !=(that: Node): Boolean = {
    id != that.getId
  }
  */
  def copy: Node = {
    new Node(id, name, docOcc, distance, category, iter)
  }

}
