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

package models.services

import com.google.inject.{ ImplementedBy, Inject }
import org.joda.time.LocalDateTime
import scalikejdbc._
import scala.collection.JavaConversions._

import models.{ Document, Facets, KeyTerm, Tag }
import util.es.{ ESRequestUtils, SearchHitIterator }
import util.RichString.richString

@ImplementedBy(classOf[FinalDocumentService])
trait DocumentService {

  def getById(docId: Long)(index: String): Option[Document]
  def getByTagLabel(label: String)(index: String): List[Document]
  def searchDocuments(facets: Facets, pageSize: Int)(index: String): (Long, Iterator[Document])

  // Document tagging
  def addTag(docId: Long, label: String)(index: String): Tag
  def removeTag(tagId: Int)(index: String): Boolean

  def getTags(docId: Long)(index: String): List[Tag]
  def getDocumentLabels()(index: String): List[String]

  // Keywords
  def getKeywords(docId: Long, size: Option[Int] = None)(index: String): List[KeyTerm]

  // Document metadata
  // TODO Add metadata abstraction and change return
  def getMetadata(docIds: List[Long], fields: List[String])(index: String): List[(Long, String, String)]
  def getMetadataKeys()(index: String): List[String]
}

// Retrieving large documents via ES is slow. We therefore use the database to fetch documents.
trait DBDocumentService extends DocumentService {

  val db = (index: String) => NamedDB(Symbol(index))

  override def getById(docId: Long)(index: String): Option[Document] = db(index).readOnly { implicit session =>
    sql"""SELECT * FROM document d
          WHERE id = $docId
      """.map(Document(_)).toOption().apply()
  }

  override def getByTagLabel(label: String)(index: String): List[Document] = {
    val tags = db(index).readOnly { implicit session =>
      sql"""SELECT t.id, t.documentid, l.label FROM tags t
            INNER JOIN labels AS l ON l.id = t.labelid
            WHERE l.label = ${label}
        """.map(Tag(_)).list().apply()
    }
    val docIds = tags.map { case Tag(_, docId, _) => docId }
    docIds.flatMap(getById(_)(index))
  }

  override def addTag(docId: Long, label: String)(index: String): Tag = db(index).localTx { implicit session =>
    val labelId = getOrCreateLabel(label)
    val tagOpt = getByValues(docId, labelId)
    tagOpt.getOrElse {
      val tagId = sql"INSERT INTO tags (documentid, labelid) VALUES (${docId}, ${labelId})".updateAndReturnGeneratedKey().apply()
      Tag(tagId, docId, label)
    }
  }

  override def removeTag(tagId: Int)(index: String): Boolean = db(index).autoCommit { implicit session =>
    val tagOpt = getTagById(tagId)
    tagOpt.exists { t =>
      val count = sql"DELETE FROM tags WHERE id = ${t.id}".update().apply()

      // Check if there are remaining tags that reference the label
      val otherTags = getByLabel(t.label)
      if (otherTags.isEmpty) {
        // We need to remove those labels
        sql"DELETE FROM labels WHERE label = ${t.label}".update().apply()
      }
      count == 1
    }
  }

  // Tag utility methods
  private def getOrCreateLabel(label: String)(implicit session: DBSession): Long = {
    val idOpt = sql"SELECT id FROM labels WHERE label = ${label}".map(_.long("id")).single().apply()
    idOpt getOrElse {
      sql"INSERT INTO labels (label) VALUES (${label})".updateAndReturnGeneratedKey().apply()
    }
  }

  private def getByValues(docId: Long, labelId: Long)(implicit session: DBSession): Option[Tag] = {
    sql"""SELECT t.id, t.documentid, l.label FROM tags t
          INNER JOIN labels AS l ON l.id = t.labelid
          WHERE t.documentid = ${docId} AND t.labelid = ${labelId}
    """.map(Tag(_)).single().apply()
  }

  private def getTagById(tagId: Long)(implicit session: DBSession): Option[Tag] = {
    sql"""SELECT t.id, t.documentid, l.label FROM tags t
          INNER JOIN labels AS l ON l.id = t.labelid
          WHERE t.id = ${tagId}
    """.map(Tag(_)).single().apply()
  }

  private def getByLabel(label: String)(implicit session: DBSession): List[Tag] = {
    sql"""SELECT t.id, t.documentid, l.label FROM tags t
          INNER JOIN labels AS l ON l.id = t.labelid
          WHERE l.label = ${label}
    """.map(Tag(_)).list().apply()
  }

  override def getTags(docId: Long)(index: String): List[Tag] = db(index).readOnly { implicit session =>
    sql"""SELECT t.id, t.documentid, l.label FROM tags t
          INNER JOIN labels AS l ON l.id = t.labelid
          WHERE t.documentid = ${docId}
    """.map(Tag(_)).list().apply()
  }

  override def getDocumentLabels()(index: String): List[String] = db(index).readOnly { implicit session =>
    sql"SELECT label FROM labels".map(_.string("label")).list().apply()
  }

  override def getKeywords(docId: Long, size: Option[Int])(index: String): List[KeyTerm] = db(index).readOnly { implicit session =>
    SQL("""SELECT term, frequency
          FROM terms
          WHERE docid = {docId}
          %s
        """.format(if (size.isDefined) "LIMIT " + size.get else "")).bindByName('docId -> docId).map(KeyTerm(_)).list.apply()
  }

  override def getMetadata(docIds: List[Long], fields: List[String])(index: String): List[(Long, String, String)] = db(index).readOnly { implicit session =>
    if (fields.nonEmpty && docIds.nonEmpty) {
      val generic = sql"""SELECT m.docid id, m.value, m.key
                          FROM metadata m
                          WHERE m.key IN (${fields}) AND m.docid IN (${docIds})
                      """.map(rs => (rs.long("id"), rs.string("key"), rs.string("value"))).list().apply()
      // Add creates fields for documents that are not explicit added as metadata
      val dates = sql"SELECT id, created FROM document WHERE id IN (${docIds})".map(rs => (rs.long("id"), "Created", rs.string("created"))).list().apply()
      dates ++ generic
    } else {
      List()
    }
  }

  override def getMetadataKeys()(index: String): List[String] = db(index).readOnly { implicit session =>
    sql"SELECT DISTINCT key, type FROM metadata".map(rs => (rs.string("key"))).list.apply()
  }
}

abstract class ESDocumentService(clientService: SearchClientService, utils: ESRequestUtils) extends DocumentService {

  private val startHighlight = "<em>"
  private val endHighlight = "</em>"

  override def searchDocuments(facets: Facets, pageSize: Int)(index: String): (Long, Iterator[Document]) = {
    val requestBuilder = utils.createSearchRequest(facets, pageSize, index, clientService)
    val highlight = requestBuilder
      .addField(utils.docContentField)
      .addHighlightedField(utils.docContentField)
      .setHighlighterNumOfFragments(0)
      .setHighlighterPreTags(startHighlight)
      .setHighlighterPostTags(endHighlight)

    val it = new SearchHitIterator(highlight)
    // TODO: We have to figure out, why this returns "4.4.0" with source name Kibana as id when we use a matchAllQuery
    (it.hits, it.flatMap { hit =>
      hit.id().toLongOpt().map { id =>
        val content = hit.field(utils.docContentField).getValue.asInstanceOf[String]
        val highlight = if (hit.highlightFields.contains(utils.docContentField)) {
          hit.highlightFields.get(utils.docContentField).fragments().headOption.map(_.toString)
        } else {
          None
        }
        Document(id, content, LocalDateTime.now, highlight)
      }
    })
  }
}

class FinalDocumentService @Inject() (clientService: SearchClientService, utils: ESRequestUtils) extends ESDocumentService(clientService, utils) with DBDocumentService
