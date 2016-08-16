/*
 * Copyright 2015 Technische Universitaet Darmstadt
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package controllers

import javax.inject.Inject

import model.Document
import model.faceted.search.{ FacetedSearch, Facets }
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc.{ Action, Controller }
import util.TimeRangeParser

// scalastyle:off
import scalikejdbc._
import util.TupleWriters._
// scalastyle:on

case class Session(hits: Long, hitIterator: Iterator[Long], hash: Long)

/*
    This class provides operations pertaining documents.
*/
object DocumentController {
  private val defaultPageSize = 50
  val facets = Facets(List(), Map(), List(), None, None)
  var res = FacetedSearch.searchDocuments(facets, defaultPageSize)
  private val defaultSssion = Session(res._1, res._2, facets.hashCode())
  private var iteratorSessions = Map("default" -> defaultSssion)
}

class DocumentController @Inject extends Controller {
  private implicit val session = AutoSession

  /**
   * returns the document with the id "id", if there is any
   */
  def getDocById(id: Int) = Action {
    Ok(Json.toJson(Document.getById(id).map(doc => (doc.id, doc.created.toString(), doc.content)))).as("application/json")
  }

  /**
   * Search for Dcoument by fulltext term and faceted search map via elastic search
   *
   * @param fullText Full text search term
   * @param generic   mapping of metadata key and a list of corresponding tags
   * @param entities list of entity ids to filter
   * @param timeRange string of a time range readable for [[TimeRangeParser]]
   * @return list of matching document id's
   */
  def getDocs(fullText: List[String], generic: Map[String, List[String]], entities: List[Long], timeRange: String) = Action { implicit request =>
    val uid = if (request.session.get("uid").nonEmpty) request.session.get("uid") else Some("0")
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val facets = Facets(fullText, generic, entities, times.from, times.to)
    var pageCounter = 0
    val metadataKeys = List("Subject", "Origin", "SignedBy", "Classification")
    var iteratorSession = DocumentController.iteratorSessions.get(uid.get)
    if (iteratorSession.isEmpty || facets.hashCode() != iteratorSession.get.hash) {
      val res = FacetedSearch.searchDocuments(facets, DocumentController.defaultPageSize)
      DocumentController.iteratorSessions += (uid.get -> Session(res._1, res._2, facets.hashCode()))
      iteratorSession = DocumentController.iteratorSessions.get(uid.get)
    }
    var docIds: List[Long] = List()
    while (iteratorSession.get.hitIterator.hasNext && pageCounter <= DocumentController.defaultPageSize) {
      docIds ::= iteratorSession.get.hitIterator.next()
      pageCounter += 1
    }
    var rs: Iterable[JsObject] = List()
    if (docIds.nonEmpty) {
      rs =
        sql"""SELECT m.docid id, m.value, m.key
        FROM metadata m
        WHERE m.key IN (${metadataKeys}) AND m.docid IN (${docIds})"""
          .map(rs => (rs.long("id"), rs.string("key"), rs.string("value")))
          .list()
          .apply()
          .groupBy(_._1)
          .map { case (id, inner) => id -> inner.map(doc => Json.obj("key" -> doc._2, "val" -> doc._3)) }
          .map(x => Json.obj("id" -> x._1, "metadata" -> Json.toJson(x._2)))
    }
    Ok(Json.toJson(Json.obj("hits" -> iteratorSession.get.hits, "docs" -> Json.toJson(rs)))).as("application/json")
  }

  /**
   * returns a list of date-number-tuples, where date is a number of milliseconds since 1970.01.01
   * and number is the amount of documents created that day
   */
  def getFrequencySeries = Action {
    val rs =
      sql"""SELECT created, COUNT(created)
                                        FROM document
                                        GROUP BY created
                                        ORDER BY created ASC"""
        .map(rs => (rs.date("created"), rs.int("count")))
        .list()
        .apply()

    // TODO: sort by data evtually
    // rs.sortWith((e1, e2) => (e1._1 < e2._1))
    Ok(Json.toJson(rs)).as("application/json")
  }

  /**
   * returns a list of all document ids from documents created at a given date and a short description
   * where date is a unixtimestamp
   */
  def getDocsByDate(date: Long) = Action {
    val rs =
      sql"""SELECT id, value
                              FROM document, metadata
                                        WHERE created = to_timestamp(${date})::date
                                        AND id = docid AND key = 'Subject'"""
        .map(rs => (rs.long("id"), rs.string("value")))
        .list()
        .apply()

    Ok(Json.toJson(rs)).as("application/json")
  }

  /**
   * Retrieves documents between two years.
   *
   * @param fromYear Select documents that have a created date >= this value.
   * @param toYear   Select documents that have a created date <= this value.
   * @param offset   The offset in the ordered list of documents
   * @param count    The amount of documents
   * @return Returns a list containing the IDs and Subjects of the documents that match the
   *         given criterion.
   */
  def getDocsForYearRange(fromYear: Int, toYear: Int, offset: Int, count: Int) = Action {
    val rs =
      sql"""SELECT id, value
                FROM document, metadata
                WHERE extract(year from created) >= ${fromYear}
                AND extract(year from created) <= ${toYear}
                AND id = docid
                AND key = 'Subject'
                ORDER BY created ASC
                LIMIT ${count}
                OFFSET ${offset}"""
        .map(rs => (rs.long("id"), rs.string("value")))
        .list()
        .apply()
    Ok(Json.toJson(rs)).as("application/json")
  }

  /**
   * Retrieves documents for a given month in a given year.
   *
   * @param year   The year to get the documents for.
   * @param month  The month to get the documents for. This value is not zero-based, thus
   *               for January pass 1.
   * @param offset The offset in the ordered list of documents
   * @param count  The amount of documents
   * @return Returns a list containing the IDs and Subjects of the documents that match the
   *         given criterion.
   */
  def getDocsForMonth(year: Int, month: Int, offset: Int, count: Int) = Action {
    val rs =
      sql"""SELECT id, value
                FROM document, metadata
                WHERE extract(year from created) = ${year}
                AND extract(month from created) = ${month}
                AND id = docid
                AND key = 'Subject'
                ORDER BY created ASC
                LIMIT ${count}
                OFFSET ${offset}"""
        .map(rs => (rs.long("id"), rs.string("value")))
        .list()
        .apply()

    Ok(Json.toJson(rs)).as("application/json")
  }

}
