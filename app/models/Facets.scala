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

import org.joda.time.LocalDateTime

// TODO Builder pattern ?
/**
 * Representation for a search query to find the most relevant documents.
 *
 * @param fullTextSearch match documents that contain the given expression in the document body.
 * @param generic a map linking from document metadata keys to a list of instances for this metadata. Different metadata
 * keys are joined via a logical ''and'', whereas different instances of the same metadata key are joined via a logical
 * ''or''.
 * @param entities a list of entity ids that should occur in the document.
 * @param fromDate start date for the document creation date (inclusive).
 * @param toDate end date for the document creation date (inclusive).
 * @param fromTimeExpression start date for the time expression mentioned in the document body (inclusive).
 * @param toTimeExpression end date for the time expresson mentioned in the document body (inclusive).
 */
case class Facets(
    fullTextSearch: List[String],
    generic: Map[String, List[String]],
    entities: List[Long],
    fromDate: Option[LocalDateTime],
    toDate: Option[LocalDateTime],
    fromTimeExpression: Option[LocalDateTime],
    toTimeExpression: Option[LocalDateTime]
) {

  def withEntities(ids: List[Long]): Facets = this.copy(entities = this.entities ++ ids)

  /** Returns ''true'' when the filter is empty i.e. it matches all documents. ''False'' otherwise. */
  def isEmpty: Boolean = fullTextSearch.isEmpty && generic.isEmpty && entities.isEmpty && !hasDateFilter

  /**
   * Returns ''true'' when the filter defines any date restriction such as the document creation date or a time expression
   * occurring in the document. ''False'' otherwise.
   */
  def hasDateFilter: Boolean = fromDate.isDefined || toDate.isDefined || fromTimeExpression.isDefined || toTimeExpression.isDefined
}

/** Companion object for [[models.Facets]]. */
object Facets {

  /** Represents an empty filter search i.e. retrieve all documents */
  val empty = Facets(List(), Map(), List(), None, None, None, None)
}

