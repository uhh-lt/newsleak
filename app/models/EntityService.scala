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

import com.google.inject.ImplementedBy
import model.Entity
import model.EntityType.withName

case class Fragment(start: Int, end: Int)

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

  override def getByIds(ids: List[Long])(index: String): List[Entity] = {
    Entity.fromDBName(index).getByIds(ids)
  }

  override def blacklist(ids: List[Long])(index: String): Boolean = {
    val entityAPI = Entity.fromDBName(index)
    ids.map(entityAPI.delete).forall(identity)
  }

  override def undoBlacklist(ids: List[Long])(index: String): Boolean = {
    val entityAPI = Entity.fromDBName(index)
    ids.map(entityAPI.undoDelete).forall(identity)
  }

  override def getBlacklisted()(index: String): List[Entity] = {
    Entity.fromDBName(index).getBlacklisted()
  }

  override def merge(focalId: Long, duplicates: List[Long])(index: String): Boolean = {
    Entity.fromDBName(index).merge(focalId, duplicates)
  }

  override def undoMerge(focalIds: List[Long])(index: String): Boolean = {
    val entityAPI = Entity.fromDBName(index)
    focalIds.map(entityAPI.undoMerge).forall(identity)
  }

  override def getMerged()(index: String): Map[Entity, List[Entity]] = {
    Entity.fromDBName(index).getDuplicates()
  }

  override def changeName(id: Long, newName: String)(index: String): Boolean = {
    Entity.fromDBName(index).changeName(id, newName)
  }

  override def changeType(id: Long, newType: String)(index: String): Boolean = {
    Entity.fromDBName(index).changeType(id, withName(newType))
  }

  override def getEntityFragments(docId: Long)(index: String): List[(Entity, Fragment)] = {
    Entity.fromDBName(index).getEntityDocumentOffsets(docId).map { case (e, start, end) => (e, Fragment(start, end)) }
  }

  override def getTypes()(index: String): List[String] = {
    Entity.fromDBName(index).getTypes().map(_.toString)
  }
}
