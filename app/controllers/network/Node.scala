/*
 *
 */
package controllers.network

import model.faceted.search.{ FacetedSearch, Facets, MetaDataBucket, NodeBucket }
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
  def createNodes(facets: Facets, nodeIds: List[Long], distToFocus: Int, iter: Int): List[Node] = {
    if (nodeIds.nonEmpty) {
      val esBuckets = FacetedSearch.fromIndexName("cable").aggregateEntities(facets, nodeIds.size, nodeIds, Nil, 1).buckets
      val nodeDocOccMap = esBuckets.collect { case NodeBucket(id, docOccurrence) => id -> docOccurrence.toInt }.toMap

      sql"""SELECT id, name, type FROM entity WHERE id IN (${nodeIds})"""
        // sql"""SELECT id, name, type FROM entity_ext WHERE id IN (${nodeIds}) AND dococc IS NOT NULL"""
        .map(rs => new Node(rs.long(1), rs.string(2), nodeDocOccMap(rs.long(1)), distToFocus, rs.string(3), iter, facets)).list.apply
    } else {
      List()
    }
  }
}

class Node(id: Long, name: String, var docOcc: Int, var distance: Int,
    category: String, var iter: Int, var facets: Facets) {
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

  def getConnectionsByType: List[(String, Long)] = {
    val typeBuckets = FacetedSearch.fromIndexName("cable").getNeighborCounts(facets, id)
    typeBuckets.buckets.collect { case MetaDataBucket(key, docCount) => (key, docCount) }
  }

  def copy: Node = {
    new Node(id, name, docOcc, distance, category, iter, facets)
  }

}