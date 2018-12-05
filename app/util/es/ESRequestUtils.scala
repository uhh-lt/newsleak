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
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.update.{ UpdateRequest, UpdateResponse }
import org.elasticsearch.script.Script

import util.DateUtils
import util.NewsleakConfigReader

// scalastyle:off
import org.elasticsearch.index.query.QueryStringQueryBuilder._
import org.elasticsearch.index.query._
// scalastyle:on
import org.joda.time.LocalDateTime

import models.services.SearchClientService
import models.Facets

import scala.collection.JavaConversions._
import scala.collection.mutable
import org.elasticsearch.common.xcontent.XContentFactory._
import play.api.libs.json._
import models.services.EntityService

/** Common helper to create and parse elasticsearch queries. Further provides elasticsearch field mappings. */
class ESRequestUtils @Inject() (
    entityService: EntityService,
    dateUtils: DateUtils
) {

  /** Elasticsearch field storing the document content. */
  val docContentField = "Content"
  /** Elasticsearch field storing the document creation date. */
  val docDateField = "Created"
  /** Elasticsearch field storing the time expression occurring in the document. */
  val docTimeExpressionField = "SimpleTimeExpresion"

  /** Elasticsearch field for running aggregation queries using entity ids. */
  val entityIdsField = "Entities" -> "Entities.EntId"
  val entityFrqField = "Entities" -> "Entities.EntFrequency"
  /** Elasticsearch field for running aggregation queries using document keywords. */
  val keywordsField = "Keywords" -> "Keywords.Keyword.raw"
  val keywordsFrqField = "Keywords" -> "Keywords.TermFrequency"

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
  def createSearchRequest(facets: Facets, documentSize: Int, index: String, client: SearchClientService, addRelevanceQuery: Boolean = false): SearchRequestBuilder = {
    val requestBuilder = client.client.prepareSearch(index)
      .setQuery(createQuery(facets, addRelevanceQuery))
      .setSize(documentSize)
      // We are only interested in the document id
      .addFields("id")

    requestBuilder
  }

  /** newsleak version 2.0.0: document whitelisting, highlighting, and keyword networks*/
  def highlightKeysByEnt(index: String, entName: String, client: SearchClientService): SearchResponse = {
    val queryBuilder = client.client.prepareSearch(index)
      .setSize(100)
      .setQuery(QueryBuilders.matchQuery("Entities.Entname", entName))
      .get()

    queryBuilder
  }

  def highlightEntsByKey(index: String, keyName: String, client: SearchClientService): SearchResponse = {
    val queryBuilder = client.client.prepareSearch(index)
      .setSize(100)
      .setQuery(QueryBuilders.boolQuery()
        .must(
          QueryBuilders.matchQuery("Keywords.Keyword.raw", keyName)
        )).get()

    queryBuilder
  }

  def multiSearchFilters(index: String, docIds: List[String], txts: List[String], kwds: List[String], ents: List[String], client: SearchClientService): SearchResponse = {

    val docs = if (docIds.size == 0) "" else if (docIds.size == 1) docIds(0) else docIds.toArray
    val texts = if (txts.size == 0) "" else if (txts.size == 1) txts(0) else txts.toArray
    val keywords = if (kwds.size == 0) "" else if (kwds.size == 1) kwds(0) else kwds.toArray
    val entities = if (ents.size == 0) "" else if (ents.size == 1) ents(0) else ents.toArray

    val queryBuilder = client.client.prepareSearch(index)
      .setQuery(QueryBuilders.boolQuery()
        .must(QueryBuilders.termsQuery("_id", docs))
        .must(
          QueryBuilders.boolQuery()
            .should(QueryBuilders.termsQuery("Content", texts))
            .should(QueryBuilders.termsQuery("Keywords.Keyword.raw", keywords))
            .should(QueryBuilders.termsQuery("Entities.EntId", entities))
        )).get()

    queryBuilder
  }

  def checkDocumentFields(index: String, docId: String, client: SearchClientService): GetResponse = {
    val requestBuilder = client.client.prepareGet(index, "document", docId)
      .get()

    requestBuilder
  }

  def createInitEntity(
    index: String,
    docId: String,
    entId: Int,
    entName: String,
    entType: String,
    client: SearchClientService
  ): UpdateResponse = {
    val updateRequest: UpdateRequest = new UpdateRequest(index, "document", docId)
      .doc(
        jsonBuilder
        .startObject()
        .startArray("Entities")
        .startObject
        .field("EntId", entId)
        .field("Entname", entName)
        .field("EntType", entType)
        .field("EntFrequency", 1)
        .endObject
        .endArray
        .endObject
      )

    val requestBuilder = client.client.update(updateRequest).get()

    requestBuilder
  }

  def createInitKeyword(
    index: String,
    docId: String,
    keyword: String,
    client: SearchClientService
  ): UpdateResponse = {
    val updateRequest: UpdateRequest = new UpdateRequest(index, "document", docId)
      .doc(
        jsonBuilder
        .startObject()
        .startArray("Keywords")
        .startObject
        .field("Keyword", keyword)
        .field("TermFrequency", 1)
        .endObject
        .endArray
        .endObject
      )

    val requestBuilder = client.client.update(updateRequest).get()

    requestBuilder
  }

  def createNewKeyword(
    index: String,
    docId: String,
    keyword: String,
    client: SearchClientService
  ): UpdateResponse = {
    val jmap = new java.util.HashMap[String, Any]()
    jmap.put("Keyword", keyword)
    jmap.put("TermFrequency", 1)

    val myScript = new Script("ctx._source.Keywords.add(json)");

    val updateRequest: UpdateRequest = new UpdateRequest(index, "document", docId).script(myScript)

    val requestBuilder = client.client.update(updateRequest).get

    requestBuilder
  }

  def createNewEntity(
    index: String,
    docId: String,
    entId: Int,
    entName: String,
    entType: String,
    client: SearchClientService
  ): UpdateResponse = {
    val jmap = new java.util.HashMap[String, Any]()
    jmap.put("EntId", entId)
    jmap.put("Entname", entName)
    jmap.put("EntType", entType)
    jmap.put("EntFrequency", 1)

    val updateRequest: UpdateRequest = new UpdateRequest(index, "document", docId).script(new Script("ctx._source.Entities.add(json)"))
    val requestBuilder = client.client.update(updateRequest).get

    requestBuilder
  }

  def createInitEntityType(
    index: String,
    docId: String,
    entId: Int,
    entName: String,
    entType: String,
    client: SearchClientService
  ): UpdateResponse = {
    val updateRequest: UpdateRequest = new UpdateRequest(index, "document", docId)
      .doc(
        jsonBuilder
        .startObject()
        .startArray("Entities" + entType)
        .startObject
        .field("EntId", entId)
        .field("Entname", entName)
        .field("EntFrequency", 1)
        .endObject
        .endArray
        .endObject
      )

    val requestBuilder = client.client.update(updateRequest).get()

    requestBuilder
  }

  def createNewEntityType(
    index: String,
    docId: String,
    entId: Int,
    entName: String,
    entType: String,
    client: SearchClientService
  ): UpdateResponse = {
    val jmap = new java.util.HashMap[String, Any]()
    jmap.put("EntId", entId)
    jmap.put("Entname", entName)
    jmap.put("EntFrequency", 1)

    val updateRequest: UpdateRequest = new UpdateRequest(index, "document", docId).script(new Script("ctx._source.Entities" + entType + ".add(json)"))
    val requestBuilder = client.client.update(updateRequest).get

    requestBuilder
  }

  private def createQuery(facets: Facets, addRelevanceQuery: Boolean = false): QueryBuilder = {
    if (facets.isEmpty) {
      QueryBuilders.matchAllQuery()
    } else {
      val request = QueryBuilders.boolQuery()

      addFulltextQuery(facets).map(request.must)
      addGenericFilter(facets).map(request.must)
      addEntitiesFilter(facets).map(request.must)
      addKeywordsFilter(facets).map(request.must)
      addDateFilter(facets).map(request.must)
      addDateXFilter(facets).map(request.must)

      if (addRelevanceQuery) {
        addEntityKeywordRelevanceQuery(facets).map(request.should)
      }

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

  private def addEntityKeywordRelevanceQuery(facets: Facets): Option[QueryStringQueryBuilder] = {
    if (facets.entities.nonEmpty || facets.keywords.nonEmpty) {
      val ids = facets.entities
      val entityList = if (ids.nonEmpty) entityService.getByIds(ids)(NewsleakConfigReader.esDefaultIndex).map(_.name).filter(e => !e.contains("/")) else List()
      val keywordList = facets.keywords.filter(e => !e.contains("/"))
      val terms = List.concat(entityList, keywordList)
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

  private def addKeywordsFilter(facets: Facets): List[TermQueryBuilder] = {
    facets.keywords.map {
      QueryBuilders.termQuery(keywordsField._2, _)
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
