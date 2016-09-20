/*
 *
 */
package controllers.network

/**
 * Created by martin on 17.09.16.
 */
class Node(id: Long, name: String, docOcc: Int, var distance: Int, category: String, var iter: Int) {
  var connectedEdges = 0

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
}
