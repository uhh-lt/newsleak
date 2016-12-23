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

import com.google.inject.Inject
import org.elasticsearch.action.search.{ SearchRequestBuilder, SearchResponse }
import util.DateUtils
// scalastyle:off
import org.elasticsearch.index.query.QueryStringQueryBuilder._
import org.elasticsearch.index.query._
// scalastyle:on
import org.joda.time.LocalDateTime

import models.services.SearchClientService
import models.Facets

/** Common helper to create and parse elasticsearch queries. Further provides elasticsearch field mappings. */
class ESRequestUtils @Inject() (dateUtils: DateUtils) {

  /** Elasticsearch field storing the document content. */
  val docContentField = "Content"
  /** Elasticsearch field storing the document creation date. */
  val docDateField = "Created"
  /** Elasticsearch field storing the time expression occurring in the document. */
  val docTimeExpressionField = "SimpleTimeExpresion"

  /** Elasticsearch field for running aggregation queries using entity ids. */
  val entityIdsField = "Entities" -> "Entities.EntId"
  /** Elasticsearch field for running aggregation queries using document keywords. */
  val keywordsField = "Keywords" -> "Keywords.Keyword.raw"

  /** Elasticsearch fields for running aggregation queries using entity ids from separate entity types. */
  def convertEntityTypeToField = (t: String) => s"Entities${t.toLowerCase}.EntId"

  /** Executes the given search request. */
  def executeRequest(request: SearchRequestBuilder, cache: Boolean = true): SearchResponse = request.setRequestCache(cache).execute().actionGet()

  /**
   * Converts an internal search query represented as [[models.Facets]] to an elasticsearch query.
   *
   * @param facets the internal search query.
   * @param documentSize the number of documents to return for the given request. Pass ''0'', if only interested in aggregation.
   * @param index the data source index or database name to query.
   * @param client the elasticsearch client.
   * @return an elasticsearch query builder.
   */
  def createSearchRequest(facets: Facets, documentSize: Int, index: String, client: SearchClientService): SearchRequestBuilder = {
    val requestBuilder = client.client.prepareSearch(index)
      .setQuery(createQuery(facets))
      .setSize(documentSize)
      // We are only interested in the document id
      .addFields("id")

    requestBuilder
  }

  private def createQuery(facets: Facets): QueryBuilder = {
    if (facets.isEmpty) {
      QueryBuilders.matchAllQuery()
    } else {
      val request = QueryBuilders.boolQuery()

      addFulltextQuery(facets).map(request.must)
      addGenericFilter(facets).map(request.must)
      addEntitiesFilter(facets).map(request.must)
      addDateFilter(facets).map(request.must)
      addDateXFilter(facets).map(request.must)

      request
    }
  }

  private def addFulltextQuery(facets: Facets): Option[QueryStringQueryBuilder] = {
    if (facets.fullTextSearch.nonEmpty) {
      // Add trailing quote if number of quotes is uneven e.g "Angela
      // ES cannot parse query otherwise.
      val terms = facets.fullTextSearch.map {
        case term if term.count(_ == '"') % 2 != 0 => term + "\""
        case term => term
      }
      val query = QueryBuilders
        .queryStringQuery(terms.mkString(" "))
        .field(docContentField)
        .defaultOperator(Operator.AND)
      Some(query)
    } else {
      None
    }
  }

  private def addGenericFilter(facets: Facets): List[BoolQueryBuilder] = {
    facets.generic.flatMap {
      case (k, v) =>
        val filter = QueryBuilders.boolQuery()
        // Query for raw field
        v.map(meta => filter.should(QueryBuilders.termQuery(s"$k.raw", meta)))
    }.toList
  }

  private def addEntitiesFilter(facets: Facets): List[TermQueryBuilder] = {
    facets.entities.map {
      QueryBuilders.termQuery(entityIdsField._2, _)
    }
  }

  private def addDateFilter(facets: Facets): Option[BoolQueryBuilder] = {
    addGenericDateFilter(docDateField, facets.fromDate, facets.toDate, dateUtils.yearMonthDayPattern)
  }

  private def addDateXFilter(facets: Facets): Option[BoolQueryBuilder] = {
    addGenericDateFilter(
      docTimeExpressionField,
      facets.fromTimeExpression,
      facets.toTimeExpression,
      s"${dateUtils.yearMonthDayPattern} || ${dateUtils.yearMonthPattern} || ${dateUtils.yearPattern}"
    )
  }

  private def addGenericDateFilter(field: String, from: Option[LocalDateTime], to: Option[LocalDateTime], dateFormat: String): Option[BoolQueryBuilder] = {
    if (from.isDefined || to.isDefined) {
      val query = QueryBuilders.boolQuery()
      val dateFilter = QueryBuilders
        .rangeQuery(field)
        .format(dateFormat)

      val gteFilter = from.map(d => dateFilter.gte(d.toString(dateUtils.yearMonthDayFormat))).getOrElse(dateFilter)
      val lteFilter = to.map(d => dateFilter.lte(d.toString(dateUtils.yearMonthDayFormat))).getOrElse(gteFilter)

      Some(query.must(lteFilter))
    } else {
      None
    }
  }
}
