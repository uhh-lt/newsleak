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
import model.Document
import model.faceted.search.{ Facets, SearchHitIterator }
import org.joda.time.LocalDateTime

import utils.RichString.richString

import scala.collection.JavaConversions._

@ImplementedBy(classOf[FinalDocumentService])
trait DocumentService {
  def getDocumentById(docId: Long)(index: String): Option[Document]
  def searchDocuments(facets: Facets, pageSize: Int)(index: String): (Long, Iterator[Document])
}

// Retrieving large documents via ES is slow. We therefore use the database to fetch documents.
trait DBDocumentService extends DocumentService {
  override def getDocumentById(docId: Long)(index: String): Option[Document] = {
    println("DB")
    None
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
