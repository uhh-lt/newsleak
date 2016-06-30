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

import model.Document
import play.api.Play.current
import play.api.Logger
import play.api.mvc.{Action, Controller}
import play.api.db._
import play.api.libs.json.Json
import play.api.libs.json.JsArray
import play.api.libs.json.Json._
import play.api.libs.json.Format
import play.api.libs.json.JsSuccess
import play.api.libs.json.Writes
import scalikejdbc.AutoSession

/*
    This class contains all those methods that are related
    to the map and its features.
*/
class MapController @Inject() extends Controller {
    private implicit val session = AutoSession

    /**
     * returns a list of all documents from documents for the given country.
     */
    def getDocsForCountry(countryCode: String) = Action{
        var list:List[String] = List()
        if (countryCode != "United States of America") {
	        // Fill with some static data until we have the database connected
	        list ::= "Yes, we can!!!!!!!!!!!!"
	        list ::= "And that is crucial!"
	        list ::= "But, it works"
	        list ::= "This is just some text for da country " + countryCode
	    }
        Ok(Json.toJson(list)).as("application/json")
    }

    // http://stackoverflow.com/questions/30921821/play-scala-json-writer-for-seq-of-tuple
    implicit def tuple3Writes[A, B, C](implicit a: Writes[A], b: Writes[B], c: Writes[C]): Writes[Tuple3[A, B, C]] = new Writes[Tuple3[A, B, C]] {
        def writes(tuple: Tuple3[A, B, C]) = JsArray(Seq(a.writes(tuple._1), b.writes(tuple._2), c.writes(tuple._3)))
    }

    def getDocsByDate(date: Long) = Action{
        var list:List[Int] = List()
        DB.withConnection { implicit connection =>
            val stmt = connection.createStatement
            val rs = stmt.executeQuery( "SELECT id "+
                                        "FROM documents "+ 
                                        "WHERE DATE(created) = "+
                                        "DATE(DATE_ADD(FROM_UNIXTIME(0),interval "+
                                        date+" second))")
            while(rs.next()){
                list ::= rs.getInt("id")
            }
        }

        Ok(Json.toJson(list)).as("application/json")
    }

}
