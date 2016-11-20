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
import scalikejdbc.WrappedResultSet

/**
 * Document representation.
 *
 * @param id       unique document identifier.
 * @param content  document body that contains raw text.
 * @param created  creation date and time of the document.
 */
case class Document(id: Long, content: String, created: LocalDateTime, highlightedContent: Option[String] = None)

/**
 * Companion object for [[Document]] instances.
 */
object Document {

  def apply(rs: WrappedResultSet): Document = Document(
    rs.int("id"),
    rs.string("content"),
    rs.jodaLocalDateTime("created")
  )
}

