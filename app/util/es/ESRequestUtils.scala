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

// scalastyle:off
import models.services.SearchClientService
import models.{ EntityType, Facets }
import org.elasticsearch.action.search.{ SearchRequestBuilder, SearchResponse }
import org.elasticsearch.index.query.QueryStringQueryBuilder._
import org.elasticsearch.index.query._
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

class ESRequestUtils {

  val docContentField = "Content"
  val docDateField = "Created"
  val docTimeExpressionField = "SimpleTimeExpresion"

  val entityIdsField = "Entities" -> "Entities.EntId"
  val keywordsField = "Keywords" -> "Keywords.Keyword.raw"

  // TODO: in course of making other entity types available, we need to adapt these hardcoded labels
  val entityTypeToField = Map(
    EntityType.Person -> "Entitiesper.EntId",
    EntityType.Organization -> "Entitiesorg.EntId",
    EntityType.Location -> "Entitiesloc.EntId",
    EntityType.Misc -> "Entitiesmisc.EntId"
  )

  val yearMonthDayPattern = "yyyy-MM-dd"
  val yearMonthPattern = "yyyy-MM"
  val yearPattern = "yyyy"

  val yearMonthDayFormat = DateTimeFormat.forPattern(yearMonthDayPattern)
  val yearMonthFormat = DateTimeFormat.forPattern(yearMonthPattern)
  val yearFormat = DateTimeFormat.forPattern(yearPattern)

  def executeRequest(request: SearchRequestBuilder, cache: Boolean = true): SearchResponse = request.setRequestCache(cache).execute().actionGet()

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
    addGenericDateFilter(docDateField, facets.fromDate, facets.toDate, yearMonthDayPattern)
  }

  private def addDateXFilter(facets: Facets): Option[BoolQueryBuilder] = {
    addGenericDateFilter(docTimeExpressionField, facets.fromTimeExpression, facets.toTimeExpression, s"$yearMonthDayPattern || $yearMonthPattern || $yearPattern")
  }

  private def addGenericDateFilter(field: String, from: Option[LocalDateTime], to: Option[LocalDateTime], dateFormat: String): Option[BoolQueryBuilder] = {
    if (from.isDefined || to.isDefined) {
      val query = QueryBuilders.boolQuery()
      val dateFilter = QueryBuilders
        .rangeQuery(field)
        .format(dateFormat)

      val gteFilter = from.map(d => dateFilter.gte(d.toString(yearMonthDayFormat))).getOrElse(dateFilter)
      val lteFilter = to.map(d => dateFilter.lte(d.toString(yearMonthDayFormat))).getOrElse(gteFilter)

      Some(query.must(lteFilter))
    } else {
      None
    }
  }
}
