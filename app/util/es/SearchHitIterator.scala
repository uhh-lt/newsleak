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

package models

import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.search.{ SearchHit, SearchHits }

/**
 * Custom implementation for paging results. ES implemented scrolling is not intended for real time user requests,
 * but rather for processing large amounts of data, e.g. in order to reindex the contents of one index into a
 * new index with a different configuration
 * See:
 * https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-scroll.html
 *
 * @param request
 */
class SearchHitIterator(request: SearchRequestBuilder) extends Iterator[SearchHit] {

  private var searchHitCounter = 0
  private var currentResultIndex = 0
  private var currentPageResults = scroll()

  lazy val hits = currentPageResults.getTotalHits

  private def scroll(): SearchHits = {
    currentResultIndex = 0
    val paginatedRequestBuilder = request.setFrom(searchHitCounter)
    val response = paginatedRequestBuilder.execute().actionGet()
    response.getHits
  }

  override def hasNext: Boolean = {
    if (currentResultIndex >= currentPageResults.getHits.length) {
      currentPageResults = scroll()
      currentPageResults.getHits.nonEmpty
    } else {
      true
    }
  }

  override def next(): SearchHit = {
    val searchHit = currentPageResults.getAt(currentResultIndex)
    searchHitCounter += 1
    currentResultIndex += 1
    searchHit
  }
}
