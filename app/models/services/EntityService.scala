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

package models.services

import com.google.inject.ImplementedBy
import models.{ Entity, EntityType, Fragment }
import scalikejdbc._

@ImplementedBy(classOf[DBEntityService])
trait EntityService {

  def getByIds(ids: List[Long])(index: String): List[Entity]

  def blacklist(ids: List[Long])(index: String): Boolean
  def undoBlacklist(ids: List[Long])(index: String): Boolean
  def getBlacklisted()(index: String): List[Entity]

  def merge(focalId: Long, duplicates: List[Long])(index: String): Boolean
  def undoMerge(focalIds: List[Long])(index: String): Boolean
  def getMerged()(index: String): Map[Entity, List[Entity]]

  def changeName(id: Long, newName: String)(index: String): Boolean
  def changeType(id: Long, newType: String)(index: String): Boolean

  def getEntityFragments(docId: Long)(index: String): List[(Entity, Fragment)]

  def getTypes()(index: String): List[String]
}

class DBEntityService extends EntityService {

  private val db = (index: String) => NamedDB(Symbol(index))

  override def getByIds(ids: List[Long])(index: String): List[Entity] = db(index).readOnly { implicit session =>
    sql"""SELECT * FROM entity
          WHERE id IN (${ids})
                AND NOT isblacklisted
          ORDER BY frequency DESC""".map(Entity(_)).list.apply()
  }

  override def blacklist(ids: List[Long])(index: String): Boolean = db(index).localTx { implicit session =>
    val entityCount = sql"UPDATE entity SET isblacklisted = TRUE WHERE id IN (${ids})".update().apply()
    entityCount == ids.sum
  }

  override def undoBlacklist(ids: List[Long])(index: String): Boolean = db(index).localTx { implicit session =>
    val entityCount = sql"UPDATE entity SET isblacklisted = FALSE WHERE id IN (${ids})".update().apply()
    // Remove entity also from the duplicates list
    val duplicateCount = sql"DELETE FROM duplicates WHERE duplicateid IN (${ids})".update().apply()
    // Successful, if updates one entity
    entityCount == ids.sum && duplicateCount == ids.sum
  }

  override def getBlacklisted()(index: String): List[Entity] = db(index).readOnly { implicit session =>
    sql"SELECT * FROM entity WHERE isblacklisted".map(Entity(_)).list.apply()
  }

  override def merge(focalId: Long, duplicates: List[Long])(index: String): Boolean = db(index).localTx { implicit session =>
    // Keep track of the origin entities for the given duplicates
    val merged = duplicates.map { id =>
      sql"INSERT INTO duplicates VALUES (${id}, ${focalId})".update.apply()
      // Blacklist duplicates in order to prevent that they show up in any query
      blacklist(List(id))(index)
    }
    merged.length == duplicates.length && merged.forall(identity)
  }

  override def undoMerge(focalIds: List[Long])(index: String): Boolean = db(index).localTx { implicit session =>
    // Remove blacklist flag from all duplicate entries with matching focalIds
    sql"""UPDATE entity
          SET isblacklisted = FALSE
          FROM duplicates
          WHERE duplicateid = id AND focalid IN (${focalIds})""".update().apply()

    sql"DELETE FROM duplicates WHERE focalid IN (${focalIds})".update().apply()
    // TODO
    true
  }

  override def getMerged()(index: String): Map[Entity, List[Entity]] = db(index).readOnly { implicit session =>
    val duplicates = sql"""SELECT e1.id, e1.name, e1.type, e1.frequency,
                                  e2.id AS focalId, e2.name AS focalName, e2.type AS focalType, e2.frequency AS focalFreq
                           FROM duplicates AS d
                           INNER JOIN entity AS e1 ON e1.id = d.duplicateid
                           INNER JOIN entity AS e2 ON e2.id = d.focalid""".map { rs =>
      (Entity(rs), Entity(
        rs.long("focalId"),
        rs.string("focalName"),
        EntityType.withName(rs.string("focalType")),
        rs.int("focalFreq")
      ))
    }.list.apply()

    duplicates.groupBy { case (_, focalEntity) => focalEntity }.mapValues(_.map(_._1))
  }

  override def changeName(id: Long, newName: String)(index: String): Boolean = db(index).localTx { implicit session =>
    val count = sql"UPDATE entity SET name = ${newName} WHERE id = ${id}".update().apply()
    // Successful, if apply updates one row
    count == 1
  }

  override def changeType(id: Long, newType: String)(index: String): Boolean = db(index).localTx { implicit session =>
    val count = sql"UPDATE entity SET type = ${newType.toString} WHERE id = ${id}".update().apply()
    count == 1
  }

  override def getEntityFragments(docId: Long)(index: String): List[(Entity, Fragment)] = {
    val fragments = db(index).readOnly { implicit session =>
      sql"""SELECT entid AS id, e.name, e.type, e.frequency, entitystart, entityend FROM entityoffset
          INNER JOIN entity AS e ON e.id = entid
          WHERE docid = ${docId}
          AND NOT e.isblacklisted
       """.map(rs => (Entity(rs), rs.int("entitystart"), rs.int("entityend"))).list.apply()
    }
    fragments.map { case (e, start, end) => (e, Fragment(start, end)) }
  }

  override def getTypes()(index: String): List[String] = db(index).readOnly { implicit session =>
    sql"SELECT DISTINCT type FROM entity WHERE NOT isblacklisted".map(rs => rs.string("type")).list.apply()
  }
}
