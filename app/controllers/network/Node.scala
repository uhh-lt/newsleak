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
class Node(id: Long, name: String, docOcc: Int, var distance: Int, category: String, var iter: Int) {
  implicit val session = AutoSession

  var connectedEdges = 0
  val newEdgesPerIter = 100
  lazy val relevantEdges = {
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

    pqIter.take(3).toList
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

  def getRelevantEdges: List[Edge] = {
    relevantEdges
  }
}
