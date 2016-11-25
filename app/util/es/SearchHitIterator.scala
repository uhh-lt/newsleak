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

package util.es

import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.search.{ SearchHit, SearchHits }

/**
 * Custom implementation for document paging.
 *
 * The elasticsearch default implementation of scrolling is not intended for real time user requests, but rather for
 * processing large amounts of data e.g. in order to re-index the contents of one index into a new index with a different
 * configuration.
 *
 * @param request the elasticsearch request defining the document filters.
 *
 * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-scroll.html]]
 */
class SearchHitIterator(request: SearchRequestBuilder) extends Iterator[SearchHit] {

  private var searchHitCounter = 0
  private var currentResultIndex = 0
  private var currentPageResults = scroll()

  /** The number of documents matching the given search query. */
  lazy val hits = currentPageResults.getTotalHits

  private def scroll(): SearchHits = {
    currentResultIndex = 0
    val paginatedRequestBuilder = request.setFrom(searchHitCounter)
    val response = paginatedRequestBuilder.execute().actionGet()
    response.getHits
  }

  /** @inheritdoc */
  override def hasNext: Boolean = {
    if (currentResultIndex >= currentPageResults.getHits.length) {
      currentPageResults = scroll()
      currentPageResults.getHits.nonEmpty
    } else {
      true
    }
  }

  /** @inheritdoc */
  override def next(): SearchHit = {
    val searchHit = currentPageResults.getAt(currentResultIndex)
    searchHitCounter += 1
    currentResultIndex += 1
    searchHit
  }
}
