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
 * Representation for an entity.
 *
 * @param id unique id and primary key of the entity.
 * @param name the entity name.
 * @param entityType the entity type e.g. ''Person'', ''Organisation'', ''Location'' or ''Miscellaneous''.
 * @param freq the document occurrence i.e. in how many documents does this entity occur.
 */
case class Entity(id: Long, name: String, entityType: String, freq: Int)

/** Companion object for [[models.Entity]] instances. */
object Entity {

  /** Automatic mapping for [[models.Entity]] to read and write from and to json. */
  implicit val entityFormat = Json.format[Entity]

  /** Factory method to create entities from database result sets. */
  def apply(rs: WrappedResultSet): Entity = Entity(
    rs.long("id"),
    rs.string("name"),
    rs.string("type"),
    rs.int("frequency")
  )
}

