/*
 *
 */
package controllers.network

import model.faceted.search.{ FacetedSearch, Facets, MetaDataBucket, NodeBucket }
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
   * @param facets Filter
   * @param nodeIds Liste von Entitaeten-Ids
   * @param distToFocus Distanz zum Fokus-Knoten
   * @param iter gibt an wie viele Guidance Schritte schon absolviert wurden
   * @return Liste von Nodes
   */
  def createNodes(facets: Facets, nodeIds: List[Long], distToFocus: Int, iter: Int)(implicit context: GraphGuidance): List[Node] = {
    val esBuckets = FacetedSearch.fromIndexName("cable").aggregateEntities(facets, nodeIds.size, nodeIds, Nil, 1).buckets
    val nodeDocOccMap = esBuckets.collect { case NodeBucket(id, docOccurrence) => id -> docOccurrence.toInt }.toMap

    sql"""SELECT id, name, type FROM entity WHERE id IN (${nodeIds})"""
      // sql"""SELECT id, name, type FROM entity_ext WHERE id IN (${nodeIds}) AND dococc IS NOT NULL"""
      .map(rs => new Node(rs.long(1), rs.string(2), nodeDocOccMap(rs.long(1)), distToFocus, rs.string(3), iter, facets)).list.apply
  }
}

class Node(id: Long, name: String, var docOcc: Int, var distance: Int,
    category: String, var iter: Int, var facets: Facets)(implicit context: GraphGuidance) {
  implicit val session = AutoSession

  val numberOfRelEdges = 4
  val newEdgesPerIter = 100

  var connectedEdges = 0

  def getId: Long = {
    id
  }

  def getName: String = {
    name
  }

  private def computeDocOcc() = {
    val esBuckets = FacetedSearch.fromIndexName("cable").aggregateEntities(facets, 1, List(id), Nil, 1).buckets
    docOcc = esBuckets.collect { case NodeBucket(nodeId, docOccurrence) => (nodeId, docOccurrence.toInt) }.head._2

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

  def update(facets: Facets, distance: Int, iter: Int) = {
    if (facets != this.facets) {
      computeDocOcc()
      this.facets = facets
    }
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
    "(Id: " + id + " Name: " + name + ")"
  }

  /**
   * @param amount Anzahl der Knoten im Iterator
   * @return Gibt einen Iterator mit Knoten zurück, die momentan nicht im generierten Subgraph vorhanden sind,
   * aber vorhanden wären, wenn von diesem Knoten aus eine Guidance gestartet würde
   */
  def getGuidancePreviewNodes(amount: Int): Iterator[Node] = {
    val ggIter = context.getCopyGuidance(id, true)
    // val ggIter = gg.getGuidance(id, context.edgeAmount, context.epn, context.uiMatrix, false, List())
    // Logger.debug(ggIter.map(_._2.get).toString())

    ggIter.take(context.edgeAmount).filterNot(t => {
      t._2.isEmpty || context.usedNodes.contains(t._2.get.getId)
    }).take(amount).map(_._2.get)

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

  def getConnectionsByType: List[(String, Long)] = {
    val typeBuckets = FacetedSearch.fromIndexName("cable").getNeighborCounts(facets, id)
    typeBuckets.buckets.collect { case MetaDataBucket(key, docCount) => (key, docCount) }
  }

  def copy: Node = {
    new Node(id, name, docOcc, distance, category, iter, facets)
  }

}