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

  def isEmpty(): Boolean = fullTextSearch.isEmpty && generic.isEmpty && entities.isEmpty && !hasDateFilter
  def hasDateFilter(): Boolean = fromDate.isDefined || toDate.isDefined || fromTimeExpression.isDefined || toTimeExpression.isDefined
}

/**
 * Companion object for Facets
 */
object Facets {

  val empty = Facets(List(), Map(), List(), None, None, None, None)
}

