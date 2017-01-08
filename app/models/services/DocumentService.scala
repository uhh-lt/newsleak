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
// scalastyle:off
import scala.collection.JavaConversions._
import scalikejdbc._
// scalastyle:on
import models.{ Document, Facets, KeyTerm, Tag }
import util.es.{ ESRequestUtils, SearchHitIterator }
import util.RichString.richString

/**
 * Defines common data access methods for retrieving and annotating documents.
 *
 * The trait is implemented by [[models.services.FinalDocumentService]], which uses mixed-in composition in order to
 * combine [[models.services.DBDocumentService]] and [[models.services.ESDocumentService]].
 */
@ImplementedBy(classOf[FinalDocumentService])
trait DocumentService {

  /**
   * Returns an option value of [[models.Document]] associated with the given id.
   *
   * @param docId the document id to search for.
   * @param index the data source index or database name to query.
   * @return [[scala.Some]] if a document with the given id exists. [[scala.None]] otherwise.
   */
  def getById(docId: Long)(index: String): Option[Document]

  /**
   * Returns a list of documents associated with the given tag label.
   *
   * @param label the tag label to search for.
   * @param index the data source index or database name to query.
   * @return a non-empty list of [[models.Document]], if at least one document annotated with the given label exists.
   * [[scala.Nil]] otherwise.
   *
   * @see See the method [[models.services.DocumentService#addTag]] to annotate documents with tags.
   */
  def getByTagLabel(label: String)(index: String): List[Document]

  /**
   * Returns an iterator with documents matching the given search query.
   *
   * The iterator acts lazy and queries for more documents once the number of consumed documents exceeds the page size.
   *
   * @param facets the search query.
   * @param pageSize number of documents per page.
   * @param index the data source index or database name to query.
   * @return a tuple consisting of the total number of hits and a document iterator for the given query.
   */
  def searchDocuments(facets: Facets, pageSize: Int)(index: String): (Long, Iterator[Document])

  /**
   * Annotates a document with the given label.
   *
   * This function is useful for grouping documents according to a label. Documents associated with the same label
   * belong to the same group. Each tag belongs exactly to one document. However, multiple tags can share the same label.
   * In case the document is already annotated with the given label the existing [[models.Tag]] is returned.
   *
   * @param docId the document id corresponding to the document to annotate.
   * @param label the label to assign.
   * @param index the data source index or database name to query.
   * @return a [[models.Tag]] representing the added tag or if already present the existing tag.
   */
  def addTag(docId: Long, label: String)(index: String): Tag

  /**
   * Removes the tag from the document associated with the given id.
   *
   * @param tagId the tag id associated with a document.
   * @param index the data source index or database name to query.
   * @return ''true'', if the tag is removed successfully. ''False'', in case the the tag does not exist.
   */
  def removeTag(tagId: Int)(index: String): Boolean

  /**
   * Returns a list of [[models.Tag]] associated with the given document id.
   *
   * @param docId the document id.
   * @param index the data source index or database name to query.
   * @return a non-empty list of [[models.Tag]], if at least one tag is associated with the given document id.
   * [[scala.Nil]] otherwise.
   */
  def getTags(docId: Long)(index: String): List[Tag]

  /**
   * Returns all distinct labels over all annotated [[models.Tag]].
   *
   * @param index the data source index or database name to query.
   * @return a non-empty list of tag labels, if at least one tag is assigned to a document. [[scala.Nil]] otherwise.
   */
  def getDocumentLabels()(index: String): List[String]

  /**
   * Returns important terms occurring in the document content for the given document id.
   *
   * @param docId the document id.
   * @param size the number of terms to fetch.
   * @param index the data source index or database name to query.
   * @return a list of [[models.KeyTerm]] representing important terms for the given document.
   * [[scala.Nil]] if no important term is associated with the given document id.
   */
  def getKeywords(docId: Long, size: Option[Int] = None)(index: String): List[KeyTerm]

  // TODO Add metadata abstraction and change return
  /**
   * Returns metadata keys and values for the given document ids.
   *
   * The function only returns metadata instances for the given keys in the field parameter.
   *
   * @param docIds a list of document ids.
   * @param fields a filter for metadata keys.
   * @param index the data source index or database name to query.
   * @return a list of triple consisting of the document id, the metadata key, value and type.
   */
  def getMetadata(docIds: List[Long], fields: List[String])(index: String): List[(Long, String, String, String)]

  /** Returns all unique metadata keys for the underlying collection. */
  def getMetadataKeys()(index: String): List[String]
}

/**
 * Partial implementation of [[models.services.DocumentService]] using a relational database.
 *
 * Retrieving large documents via ES is slow. We therefore use the database to fetch documents. Further, user-generated
 * data like tags are stored in the database as well.
 */
trait DBDocumentService extends DocumentService {

  private val db = (index: String) => NamedDB(Symbol(index))

  /** @inheritdoc */
  override def getById(docId: Long)(index: String): Option[Document] = db(index).readOnly { implicit session =>
    sql"""SELECT * FROM document d
          WHERE id = $docId
      """.map(Document(_)).toOption().apply()
  }

  /** @inheritdoc */
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

  /** @inheritdoc */
  override def addTag(docId: Long, label: String)(index: String): Tag = db(index).localTx { implicit session =>
    val labelId = getOrCreateLabel(label)
    val tagOpt = getByValues(docId, labelId)
    tagOpt.getOrElse {
      val tagId = sql"INSERT INTO tags (documentid, labelid) VALUES (${docId}, ${labelId})".updateAndReturnGeneratedKey().apply()
      Tag(tagId, docId, label)
    }
  }

  /** @inheritdoc */
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

  /** @inheritdoc */
  override def getTags(docId: Long)(index: String): List[Tag] = db(index).readOnly { implicit session =>
    sql"""SELECT t.id, t.documentid, l.label FROM tags t
          INNER JOIN labels AS l ON l.id = t.labelid
          WHERE t.documentid = ${docId}
    """.map(Tag(_)).list().apply()
  }

  /** @inheritdoc */
  override def getDocumentLabels()(index: String): List[String] = db(index).readOnly { implicit session =>
    sql"SELECT label FROM labels".map(_.string("label")).list().apply()
  }

  /** @inheritdoc */
  override def getKeywords(docId: Long, size: Option[Int])(index: String): List[KeyTerm] = db(index).readOnly { implicit session =>
    SQL("""SELECT term, frequency
          FROM terms
          WHERE docid = {docId}
          %s
        """.format(if (size.isDefined) "LIMIT " + size.get else "")).bindByName('docId -> docId).map(KeyTerm(_)).list.apply()
  }

  /** @inheritdoc */
  override def getMetadata(
    docIds: List[Long],
    fields: List[String]
  )(index: String): List[(Long, String, String, String)] = db(index).readOnly { implicit session =>
    if (fields.nonEmpty && docIds.nonEmpty) {
      val generic = sql"""SELECT m.docid id, m.value, m.key, m.type
                          FROM metadata m
                          WHERE m.key IN (${fields}) AND m.docid IN (${docIds})
                      """.map(rs => (rs.long("id"), rs.string("key"), rs.string("value"), rs.string("type"))).list().apply()
      // Add creates fields for documents that are not explicit added as metadata
      val dates = sql"SELECT id, created FROM document WHERE id IN (${docIds})".map(rs => (rs.long("id"), "Created", rs.string("created"), "Date")).list().apply()
      dates ++ generic
    } else {
      List()
    }
  }

  /** @inheritdoc */
  override def getMetadataKeys()(index: String): List[String] = db(index).readOnly { implicit session =>
    sql"SELECT DISTINCT key, type FROM metadata".map(rs => (rs.string("key"))).list.apply()
  }
}

/**
 * Partial implementation of [[models.services.DocumentService]] using an elasticsearch index as backend.
 *
 * @param clientService the elasticsearch client.
 * @param utils common helper to issue elasticsearch queries.
 */
abstract class ESDocumentService(clientService: SearchClientService, utils: ESRequestUtils) extends DocumentService {

  private val startHighlight = "<em>"
  private val endHighlight = "</em>"

  /** @inheritdoc */
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

/**
 * Combines the elasticsearch and relational database implementations via mixed-in composition.
 *
 * @param clientService the elasticsearch client interface.
 * @param utils common helper to issue elasticsearch queries.
 */
class FinalDocumentService @Inject() (clientService: SearchClientService, utils: ESRequestUtils)
  extends ESDocumentService(clientService, utils) with DBDocumentService
