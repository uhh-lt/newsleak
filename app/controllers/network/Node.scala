/*
 *
 */
package controllers.network

/**
 * Created by martin on 17.09.16.
 */
class Node(id: Long, docOcc: Int, var distance: Int, category: String = "unknown",var iter: Int) {

  def getId: Long = {
    id
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

  def update(distance: Int, iter: Int) = {
    if (iter == this.iter)
      if (distance < this.distance) {
        // TODO Kanten müssen rekursiv geupdatet werden
        this.distance = distance
      }
      else {
        this.distance = distance
      }
  }

  override def toString: String = {
    "(S Id: " + id + ")"
  }
}
