/*
 * Copyright 2015 Technische Universitaet Darmstadt
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package controllers

import javax.inject.Inject

import play.api.Play.current
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import scalikejdbc.AutoSession

// scalastyle:off
import util.TupleWriters._
import play.api.db._
// scalastyle:on

/*
    This class contains all those methods that are related
    to the map and its features.
*/
class MapController @Inject extends Controller {
  private implicit val session = AutoSession

  /**
   * returns a list of all documents from documents for the given country.
   */
  def getDocsForCountry(countryCode: String) = Action {
    var list: List[String] = List()
    if (countryCode != "United States of America") {
      // Fill with some static data until we have the database connected
      list ::= "Yes, we can!!!!!!!!!!!!"
      list ::= "And that is crucial!"
      list ::= "But, it works"
      list ::= "This is just some text for da country " + countryCode
    }
    Ok(Json.toJson(list)).as("application/json")
  }

  def getDocsByDate(date: Long) = Action {
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
