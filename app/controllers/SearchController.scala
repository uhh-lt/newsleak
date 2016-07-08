package controllers

import play.api.mvc.{Action, Results, Controller}
import model.{Entity, Tag}
import play.api.libs.json.{JsArray, JsObject, Json, Writes}
import javax.inject.Inject



/**
  * Created by patrick on 19.04.16.
  */
class SearchController @Inject extends Controller {
	// http://stackoverflow.com/questions/30921821/play-scala-json-writer-for-seq-of-tuple
	implicit def tuple2Writes[A, B](implicit a: Writes[A], b: Writes[B]): Writes[Tuple2[A, B]] = new Writes[Tuple2[A, B]] {
		def writes(tuple: Tuple2[A, B]) = JsArray(Seq(a.writes(tuple._1), b.writes(tuple._2)))
	}

	// http://stackoverflow.com/questions/30921821/play-scala-json-writer-for-seq-of-tuple
	implicit def tuple3Writes[A, B, C](implicit a: Writes[A], b: Writes[B], c: Writes[C]): Writes[Tuple3[A, B, C]] = new Writes[Tuple3[A, B, C]] {
		def writes(tuple: Tuple3[A, B, C]) = JsArray(Seq(a.writes(tuple._1),
			b.writes(tuple._2),
			c.writes(tuple._3)))
	}

	/**
	  * get the autocomplete tags to this query
	  * @param query the query to get the autocomplete
	  *              tags for
	  * @param limit maximum number of items to send
	  *              to the client
	  * @return
	  *         an array of entity names and entity types
	  *         combined
	  */
	def getAutocomplete(query: String/*, limit:Int*/) = Action{
		Results.Ok(Json.obj("entities" ->  Entity.getByNamePattern(query).map(entity => (entity.id, entity.name, entity.entityType.toString)))).as("application/json")
	}
}
