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

import play.api.libs.json.Json
import scalikejdbc.WrappedResultSet

/**
 * Representation for important terms including their importance value.
 *
 * @param term the important term value.
 * @param score the score of the important term. Higher values represent more important terms.
 */
case class KeyTerm(term: String, score: Int)

/** Companion object for [[models.KeyTerm]] instances. */
object KeyTerm {

  /** Automatic mapping for [[models.KeyTerm]] to read and write from and to json. */
  implicit val keyTermFormat = Json.format[KeyTerm]

  /** Factory method to create key terms from database result sets. */
  def apply(rs: WrappedResultSet): KeyTerm = KeyTerm(rs.string("term"), rs.int("frequency"))
}
