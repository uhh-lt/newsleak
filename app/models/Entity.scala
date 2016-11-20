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

import scalikejdbc.WrappedResultSet

/**
 * Entity type (<tt>Person</tt>, <tt>Organisation</tt>, <tt>Location</tt>).
 */
object EntityType extends Enumeration {
  val Person = Value("PER")
  val Organization = Value("ORG")
  val Location = Value("LOC")
  val Misc = Value("MISC")
}

/**
 * Representation for entities.
 *
 * @param id unique id and primary key of the entity.
 * @param name the entity name.
 * @param entityType the entity type.
 * @param frequency the frequency (i.e. co-occurrence) in the underlying data.
 */
case class Entity(id: Long, name: String, entityType: EntityType.Value, frequency: Int)

/**
 * Companion object for [[Entity]] instances.
 */
object Entity {

  def apply(rs: WrappedResultSet): Entity = Entity(
    rs.long("id"),
    rs.string("name"),
    EntityType.withName(rs.string("type")),
    rs.int("frequency")
  )
}

