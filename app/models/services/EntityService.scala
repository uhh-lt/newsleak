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
// scalastyle:off
import scalikejdbc._
// scalastyle:on
import models.{ Entity, EntityType, Fragment }

/**
 * Defines common data access methods for retrieving and manipulating [[models.Entity]].
 *
 * The trait is implemented by [[models.services.DBDocumentService]].
 */
@ImplementedBy(classOf[DBEntityService])
trait EntityService {

  /**
   * Returns a list of [[models.Entity]] matching the given entity ids.
   *
   * @param ids a list of entity ids.
   * @param index the data source index or database name to query.
   * @return a list of [[models.Entity]] corresponding to the given ids or [[scala.Nil]] if no
   * matching entity is found.
   */
  def getByIds(ids: List[Long])(index: String): List[Entity]

  /**
   * Marks the entities associated with the given ids as blacklisted.
   *
   * Blacklisted entities don't appear in any result set.
   *
   * @param ids the entity ids to blacklist.
   * @param index the data source index or database name to query.
   * @return ''true'', if all entities are successfully marked as blacklisted. ''False'' if at least one entity
   * is not correct marked.
   */
  def blacklist(ids: List[Long])(index: String): Boolean

  /**
   * Removes the blacklisted mark from the entities associated with the given ids.
   *
   * After executing this operation, the respective entities do appear in result sets again.
   *
   * @param ids the entity ids to remove the blacklist mark from.
   * @param index the data source index or database name to query.
   * @return ''true'', if the blacklist mark is successfully removed from all entities. ''False'' if at least one blacklist
   * mark for an entity is not correct removed.
   */
  def undoBlacklist(ids: List[Long])(index: String): Boolean

  /**
   * Returns all blacklisted entities for the underlying collection.
   *
   * @param index the data source index or database name to query.
   * @return a list of [[models.Entity]], where each entity is marked as blacklisted.
   */
  def getBlacklisted()(index: String): List[Entity]

  /**
   * Merges multiple nodes in a given focal node.
   *
   * The duplicates don't appear in any result set anymore. Further, any entity-related search using the focal node as
   * instance also queries for its duplicates i.e. searching for "Angela Merkel" will also search for "Angela" or "Dr. Merkel".
   *
   * @param focalId the central entity id.
   * @param duplicates entity ids referring to similar textual mentions of the focal id.
   * @param index the data source index or database name to query.
   * @return ''true'', if the operation was successful. ''False'' otherwise.
   */
  def merge(focalId: Long, duplicates: List[Long])(index: String): Boolean

  /**
   * Withdraws [[models.services.EntityService#merge]] for the given entity id.
   *
   * @param focalIds the central entity id.
   * @param index the data source index or database name to query.
   * @return ''true'', if the removal was successful. ''False'' otherwise.
   */
  def undoMerge(focalIds: List[Long])(index: String): Boolean

  /**
   * Returns all merged entities for the underlying collection.
   *
   * @param index the data source index or database name to query.
   * @return a map linking from the focal entity to its duplicates.
   */
  def getMerged()(index: String): Map[Entity, List[Entity]]

  /**
   * Changes the name of the entity corresponding to the given entity id.
   *
   * @param id the entity id to change.
   * @param newName the new name to apply.
   * @param index the data source index or database name to query.
   * @return ''true'', if the operation was successful. ''False'' otherwise.
   */
  def changeName(id: Long, newName: String)(index: String): Boolean

  /**
   * Changes the type of the entity corresponding to the given entity id.
   *
   * @param id the entity id to change.
   * @param newType the new type to apply.
   * @param index the data source index or database name to query.
   * @return ''true'', if the operation was successful. ''False'' otherwise.
   */
  def changeType(id: Long, newType: String)(index: String): Boolean

  /**
   * Returns all entity occurrences for the given document including their position in the document.
   *
   * @param docId the document id.
   * @param index the data source index or database name to query.
   * @return a list of tuple consisting of an entity and its position in the document.
   */
  def getEntityFragments(docId: Long)(index: String): List[(Entity, Fragment)]

  /** Returns a list of distinct entity types in the underlying collection */
  def getTypes()(index: String): List[String]
}

/** Implementation of [[models.services.EntityService]] using a relational database. */
class DBEntityService extends EntityService {

  private val db = (index: String) => NamedDB(Symbol(index))

  /** @inheritdoc */
  override def getByIds(ids: List[Long])(index: String): List[Entity] = db(index).readOnly { implicit session =>
    sql"""SELECT * FROM entity
          WHERE id IN (${ids})
                AND NOT isblacklisted
          ORDER BY frequency DESC""".map(Entity(_)).list.apply()
  }

  /** @inheritdoc */
  override def blacklist(ids: List[Long])(index: String): Boolean = db(index).localTx { implicit session =>
    val entityCount = sql"UPDATE entity SET isblacklisted = TRUE WHERE id IN (${ids})".update().apply()
    entityCount == ids.sum
  }

  /** @inheritdoc */
  override def undoBlacklist(ids: List[Long])(index: String): Boolean = db(index).localTx { implicit session =>
    val entityCount = sql"UPDATE entity SET isblacklisted = FALSE WHERE id IN (${ids})".update().apply()
    // Remove entity also from the duplicates list
    val duplicateCount = sql"DELETE FROM duplicates WHERE duplicateid IN (${ids})".update().apply()
    // Successful, if updates one entity
    entityCount == ids.sum && duplicateCount == ids.sum
  }

  /** @inheritdoc */
  override def getBlacklisted()(index: String): List[Entity] = db(index).readOnly { implicit session =>
    sql"SELECT * FROM entity WHERE isblacklisted".map(Entity(_)).list.apply()
  }

  /** @inheritdoc */
  override def merge(focalId: Long, duplicates: List[Long])(index: String): Boolean = db(index).localTx { implicit session =>
    // Keep track of the origin entities for the given duplicates
    val merged = duplicates.map { id =>
      sql"INSERT INTO duplicates VALUES (${id}, ${focalId})".update.apply()
      // Blacklist duplicates in order to prevent that they show up in any query
      blacklist(List(id))(index)
    }
    merged.length == duplicates.length && merged.forall(identity)
  }

  /** @inheritdoc */
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

  /** @inheritdoc */
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

  /** @inheritdoc */
  override def changeName(id: Long, newName: String)(index: String): Boolean = db(index).localTx { implicit session =>
    val count = sql"UPDATE entity SET name = ${newName} WHERE id = ${id}".update().apply()
    // Successful, if apply updates one row
    count == 1
  }

  /** @inheritdoc */
  override def changeType(id: Long, newType: String)(index: String): Boolean = db(index).localTx { implicit session =>
    val count = sql"UPDATE entity SET type = ${newType.toString} WHERE id = ${id}".update().apply()
    count == 1
  }

  /** @inheritdoc */
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

  /** @inheritdoc */
  override def getTypes()(index: String): List[String] = db(index).readOnly { implicit session =>
    sql"SELECT DISTINCT type FROM entity WHERE NOT isblacklisted".map(rs => rs.string("type")).list.apply()
  }
}
