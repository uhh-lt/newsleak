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

package controllers

import javax.inject.Inject

import play.api.Play.current
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import scalikejdbc.{ AutoSession, DBSession }

// scalastyle:off
import util.TupleWriters._
import play.api.db._
// scalastyle:on

/*
    This class contains all those methods that are related
    to the map and its features.
*/
class MapController @Inject extends Controller {

  /**
   * returns a list of all documents from documents for the given country.
   */
  def getDocsForCountry(countryCode: String) = Action {
    Ok(Json.toJson(Json.obj())).as("application/json")
  }

  def getDocsByDate(date: Long)(implicit session: DBSession = AutoSession) = Action {
    var list: List[Int] = List()
    DB.withConnection { implicit connection =>
      val stmt = connection.createStatement
      val rs = stmt.executeQuery("SELECT id " +
        "FROM documents " +
        "WHERE DATE(created) = " +
        "DATE(DATE_ADD(FROM_UNIXTIME(0),interval " +
        date + " second))")
      while (rs.next()) {
        list ::= rs.getInt("id")
      }
    }

    Ok(Json.toJson(list)).as("application/json")
  }

}
