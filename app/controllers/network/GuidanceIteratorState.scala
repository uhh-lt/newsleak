/*

 */
package controllers.network

import model.faceted.search.Facets

import scala.collection.mutable
import scala.math.Ordering

/**
 * Created by martin on 20.10.16.
 */
class GuidanceIteratorState(
    var nodeList: mutable.HashMap[Long, mutable.PriorityQueue[Edge]],
    var pq: mutable.PriorityQueue[Edge],
    var usedEdges: mutable.HashMap[(Long, Long), Edge],
    var usedNodes: mutable.HashMap[Long, Node],

    var facets: Facets,
    var edgeAmount: Int,
    var epn: Int,
    var uiMatrix: Array[Array[Int]],
    var useOldEdges: Boolean
) {
  implicit val order = Ordering.by[Edge, (Double, Long, Long)](e => (
    e.getDoi, e.getNodes._1.getId, e.getNodes._2.getId
  ))

  def copy: GuidanceIteratorState =
    new GuidanceIteratorState(
      nodeList.map(p => p._1 -> (new mutable.PriorityQueue() ++= p._2.map(e => e.copy))),
      new mutable.PriorityQueue() ++= pq.map(e => e.copy),
      usedEdges.map(e => e._1 -> e._2.copy),
      usedNodes.map(n => n._1 -> n._2.copy),
      facets.copy(),
      edgeAmount,
      epn,
      uiMatrix,
      true
    )
}