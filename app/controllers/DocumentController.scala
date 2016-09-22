/*
 * Copyright (C) 2016 Language Technology Group and Interactive Graphics Systems Group, Technische Universität Darmstadt, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package controllers

import javax.inject.Inject

import model.Document
import model.faceted.search.{ FacetedSearch, Facets }
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.{ Action, Controller }
import util.SessionUtils.currentDataset
import util.TimeRangeParser

// scalastyle:off
import play.cache._
import util.TupleWriters._
// scalastyle:on

case class IteratorSession(hits: Long, hitIterator: Iterator[Long], hash: Long)

/*
    This class provides operations pertaining documents.
*/
class DocumentController @Inject() (cache: CacheApi) extends Controller {

  private val defaultPageSize = 50
  private val defaultFacets = Facets.emptyFacets
  private val (numberOfDocuments, documentIterator) = FacetedSearch.fromIndexName("cable").searchDocuments(defaultFacets, defaultPageSize)
  private val defaultSession = IteratorSession(numberOfDocuments, documentIterator, defaultFacets.hashCode())
  private val metadataKeys = List("Subject", "Origin", "SignedBy", "Classification")
  // metdatakeys for enron
  // private val metadataKeys = List("Subject", "Timezone", "sender.name", "Recipients.email", "Recipients.name", "Recipients.type")

  /**
   * returns the document with the id "id", if there is any
   */
  def getDocById(id: Int) = Action { implicit request =>
    val docSearch = Document.fromDBName(currentDataset)
    Ok(Json.toJson(docSearch.getById(id).map(doc => (doc.id, doc.created.toString(), doc.content)))).as("application/json")
  }

  /**
   * Search for Document by fulltext term and faceted search map via elasticsearch
   *
   * @param fullText full-text search term
   * @param generic   mapping of metadata key and a list of corresponding tags
   * @param entities list of entity ids to filter
   * @param timeRange string of a time range readable for [[TimeRangeParser]]
   * @return list of matching document id's
   */
  def getDocs(
    fullText: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    timeRange: String
  ) = Action { implicit request =>
    val uid = request.session.get("uid").getOrElse("0")
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val facets = Facets(fullText, generic, entities, times.from, times.to)
    var pageCounter = 0

    val facetSearch = FacetedSearch.fromIndexName(currentDataset)
    var iteratorSession: IteratorSession = cache.get[IteratorSession](uid)
    if (iteratorSession == null || iteratorSession.hash == defaultSession.hash || iteratorSession.hash != facets.hashCode()) {
      val res = facetSearch.searchDocuments(facets, defaultPageSize)
      iteratorSession = IteratorSession(res._1, res._2, facets.hashCode())
      cache.set(uid, iteratorSession)
    }

    var docIds: List[Long] = List()
    while (iteratorSession.hitIterator.hasNext && pageCounter <= defaultPageSize) {
      docIds ::= iteratorSession.hitIterator.next()
      pageCounter += 1
    }
    if (docIds.size < 50) {
      val newIteratorSession = IteratorSession(iteratorSession.hits, iteratorSession.hitIterator, -1)
      cache.set(uid, newIteratorSession)
    }
    if (docIds.nonEmpty) {
      val docSearch = Document.fromDBName(currentDataset)

      val metadataTriple = docSearch.getMetadataForDocuments(docIds, metadataKeys)
      val response = metadataTriple
        .groupBy(_._1)
        .map { case (id, inner) => id -> inner.map(doc => Json.obj("key" -> doc._2, "val" -> doc._3)) }
        .map(x => Json.obj("id" -> x._1, "metadata" -> Json.toJson(x._2)))

      Ok(Json.toJson(Json.obj("hits" -> iteratorSession.hits, "docs" -> Json.toJson(response)))).as("application/json")
    } else {
      // No documents found for given facets
      Ok(Json.toJson(Json.obj("hits" -> 0, "docs" -> List[JsObject]()))).as("application/json")
    }
  }
}
