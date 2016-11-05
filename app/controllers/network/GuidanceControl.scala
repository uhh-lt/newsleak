/*

  */
package controllers.network

import play.api.libs.json._
import play.api.Logger

import scala.collection.mutable

case class State(private val gg: GraphGuidance, private val ggIterState: GuidanceIteratorState, private val lastOutput: JsObject) {
  def ggIter: GuidanceIterator = gg.createIterator.initWithState(ggIterState.copy)
  def guidance: GraphGuidance = gg.copy
  def output: JsObject = {
    Logger.debug(gg.uiMatrix.map(_.mkString(",")).mkString(";"))
    lastOutput ++ new JsObject(Map(
      "focusId" -> Json.toJson(gg.focusNodeId),
      "uiMatrix" -> JsString(gg.uiMatrix.map(_.mkString(",")).mkString(";")),
      "epn" -> JsNumber(gg.epn),
      "edgeAmount" -> JsNumber(gg.edgeAmount)
    ))
  }
}
object State {
  val initial = State(new GraphGuidance, null, new JsObject(Map()))
}

/**
 * Created by martin on 19.10.16.
 */
class GuidanceControl {
  private var currentState: State = State.initial
  private val undoStack = new mutable.Stack[State]
  private val redoStack = new mutable.Stack[State]

  def getState: State = currentState

  def undoAvailable: Boolean = undoStack.nonEmpty

  def redoAvailable: Boolean = redoStack.nonEmpty

  def undo: State = {
    redoStack.push(currentState)
    currentState = undoStack.pop()
    Logger.debug(currentState.output.toString())
    getState
  }

  def redo: State = {
    undoStack.push(currentState)
    currentState = redoStack.pop()
    Logger.debug(currentState.output.toString())
    getState
  }

  def addState(state: State) = {
    if (currentState != State.initial) {
      undoStack.push(currentState)
    }
    redoStack.clear()
    currentState = state
  }

  def updateState(state: State) = {
    currentState = state
  }

}
