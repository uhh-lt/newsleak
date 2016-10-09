/*

 */
package controllers.network

/**
 * Created by martin on 08.10.16.
 */
trait GuidanceIterator extends Iterator[(Edge, Option[Node])] {
  def getMoreEdges(nodeId: Long, amount: Int): (List[Edge], List[Node])
}
