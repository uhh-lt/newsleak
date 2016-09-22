/*

 */
package controllers.network

import math.log
import math.pow
import math.min

/**
 * Created by martin on 17.09.16.
 */
class Edge(n1: Node, n2: Node, docOcc: Int, ui: Double, oldDoi: Double) {
  private val docs = 251287 // Anzahl der Dokumente in der SQL DB

  // Logger.info("" + n1 + n2)
  // assert(n1.getId < n2.getId)
  // val id: Double = (n1.getId:Double) + math.pow(n2.getId:Double,7)
  private var dist = min(n1.getDistance, n2.getDistance)
  private var doiDebugString = ""
  private val doi = if (ui == Double.NegativeInfinity) {
    Double.NegativeInfinity // Wenn Kanten dieses Types nicht berücksichtig werden sollen, setze den DoI Wert auf -inf
  } else {
    val pmi2 = log(((docOcc: Double) / docs * (docOcc: Double) / docs) / ((n1.getDocOcc: Double) / docs * (n2.getDocOcc: Double) / docs))
    val npmi2 = pmi2 / (-log((docOcc: Double) / docs * (docOcc: Double) / docs))
    val npmi2plus = (npmi2 + 1) / 2

    val alpha = 1
    val beta = 1
    val gamma = 1

    val API = npmi2plus
    val D = -(1 - pow(0.5, dist)) * npmi2plus
    val UI = npmi2plus * ui + oldDoi * 0.1

    doiDebugString = " api: " + API
    API * alpha + beta * D + UI * gamma
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

  def toString(showNodes: Boolean = false): String = {
    (if (showNodes) { n1.toString + n2.toString } else { "" }) + "doi: " + doi + doiDebugString
  }

  override def toString: String = toString(false)

}
