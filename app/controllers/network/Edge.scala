/*

 */
package controllers.network

import scala.math.log

/**
 * Created by martin on 17.09.16.
 */
class Edge(n1: Node, n2: Node, docOcc: Int, ui: Double) {
  // Logger.info("" + n1 + n2)
  // assert(n1.getId < n2.getId)
  // val id: Double = (n1.getId:Double) + math.pow(n2.getId:Double,7)
  private var api = 1 / -log((docOcc: Double) * (docOcc: Double) / ((n1.getDocOcc: Double) * n2.getDocOcc: Double))
  private var dist = math.min(n1.getDistance, n2.getDistance)
  private val doi = if (ui == Double.NegativeInfinity) {
    Double.NegativeInfinity // Wenn Kanten dieses Types nicht berücksichtig werden sollen, setze den DoI Wert auf -inf
  } else {
    val alpha = 1
    val beta = 0
    val gamma = 1

    val D = 0 // -(1 - scala.math.pow(0.5, distToFocus) * (x._2))

    val UI = api * ui
    api * alpha + beta * D + UI * gamma
  }

  def getNodes: (Node, Node) = {
    (n1, n2)
  }
  def getDocOcc: Int = {
    docOcc
  }

  def getDoi: Double = {
    doi
  }

  override def toString: String = {
    n1.toString + n2.toString + "doi: " + doi
  }

}
