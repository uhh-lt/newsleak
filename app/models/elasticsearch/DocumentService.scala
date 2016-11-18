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

package models.elasticsearch

// scalastyle:off
import com.google.inject.{ ImplementedBy, Inject }
import model.{ Document, KeyTerm, Tag }
import model.faceted.search.{ Facets, SearchHitIterator }
import org.joda.time.LocalDateTime
import util.SessionUtils._
import utils.RichString.richString

import scala.collection.JavaConversions._

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

  override def getById(docId: Long)(index: String): Option[Document] = {
    Document.fromDBName(index).getById(docId)
  }

  override def getByTagLabel(label: String)(index: String): List[Document] = {
    val docIds = Tag.fromDBName(index).getByLabel(label).map { case Tag(_, docId, _) => docId }
    docIds.flatMap(getById(_)(index))
  }

  override def addTag(docId: Long, label: String)(index: String): Tag = {
    Tag.fromDBName(index).add(docId, label)
  }

  override def removeTag(tagId: Int)(index: String): Boolean = {
    Tag.fromDBName(index).delete(tagId)
  }

  override def getTags(docId: Long)(index: String): List[Tag] = {
    Tag.fromDBName(index).getByDocumentId(docId)
  }

  override def getDocumentLabels()(index: String): List[String] = {
    Tag.fromDBName(index).getDistinctLabels()
  }

  override def getKeywords(docId: Long, size: Option[Int])(index: String): List[KeyTerm] = {
    KeyTerm.fromDBName(index).getDocumentKeyTerms(docId, size)
  }

  override def getMetadata(docIds: List[Long], fields: List[String])(index: String): List[(Long, String, String)] = {
    Document.fromDBName(index).getMetadataForDocuments(docIds, fields)
  }

  override def getMetadataKeys()(index: String): List[String] = {
    Document.fromDBName(index).getMetadataKeysAndTypes().map(_._1)
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
