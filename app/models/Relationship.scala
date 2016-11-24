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

// scalastyle:off
import play.api.libs.json._
// scalastyle:on

/**
 * Representation for a relationship.
 *
 * @param source the first adjacent node.
 * @param dest the second adjacent node.
 * @param occurrence the document occurrence i.e. in how many documents does this relationship occur.
 */
case class Relationship(source: Long, dest: Long, occurrence: Long)

/** Companion object for [[models.Relationship]] instances. */
object Relationship {

  /** Automatic mapping for [[models.Relationship]] to read and write from and to json. */
  implicit val relationshipFormat = Json.format[Relationship]
}