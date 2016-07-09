package controllers

import javax.inject.Inject

import model.faceted.search.{FacetedSearch, MetaDataBucket}
import model.{Document, Entity, EntityType}
import play.api.libs.json.{JsArray, JsObject, Json, Writes}
import play.api.mvc.{Action, Controller, Results}

/**
  * Created by f. zouhar on 26.05.16.
  */
class MetadataController @Inject extends Controller {
  // http://stackoverflow.com/questions/30921821/play-scala-json-writer-for-seq-of-tuple
  implicit def tuple2Writes[A, B](implicit a: Writes[A], b: Writes[B]): Writes[Tuple2[A, B]] = new Writes[Tuple2[A, B]] {
    def writes(tuple: Tuple2[A, B]) = JsArray(Seq(a.writes(tuple._1), b.writes(tuple._2)))
  }


  //TODO: hardcoded types because we need only 4 in overview
  val types = List(("Origin","String"), ("Tags","String"), ("SignedBy","String"),("Classification","String"))

  /**
    * Get all metadata types
    * @return list of metadata types
    */
  def getMetadataTypes =  Action {
    Results.Ok(Json.toJson(types.map(x => x._1))).as("application/json")
    //Results.Ok(Json.toJson(Document.getMetadataKeysAndTypes().map(x => x._1))).as("application/json")
  }

  /**
    * Gets document counts for all metadata types corresponding to their keys
    * @param fullText Full text search term
    * @param facets mapping of metadata key and a list of corresponding tags
    * @return list of matching metadata keys and document count
    */
  def getMetadata(fullText: Option[String], facets: Map[String, List[String]]) = Action {
    //val types = Document.getMetadataKeysAndTypes()
    var res: List[JsObject] = List()
    types.foreach(metadataType => {
      val agg = FacetedSearch.aggregate(fullText, facets, metadataType._1, 50)
      res ::= Json.obj(metadataType._1 -> agg.get.buckets.map(x => x match {
        case MetaDataBucket(key, count) => Json.obj("key" -> key, "count" -> count)
        case _ => Json.obj()
      }))
    })

    Results.Ok(Json.toJson(res)).as("application/json")
  }

  /**
    * Gets document counts for keywords
    * @param fullText Full text search term
    * @param facets mapping of metadata key and a list of corresponding tags
    * @return list of matching keywords and document count
    */
  def getKeywords(fullText: Option[String], facets: Map[String, List[String]]) = Action {
      val res = FacetedSearch.aggregateKeywords(fullText, facets, 50).buckets.map(x => x match {
        case MetaDataBucket(key, count) => Json.obj("key" -> key, "count" -> count)
        case _ => Json.obj()
      })

    Results.Ok(Json.toJson(res)).as("application/json")
  }
}
