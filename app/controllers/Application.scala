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

import play.api.Logger
import play.api.mvc.{ Action, Controller }
import org.apache.commons.codec.binary.{ Base64, Hex, StringUtils }
import javax.inject.Inject

import scala.util.Random

/*
  This class define which URL patterns match which views as well as
  which play routes exist and where they point at.
*/
class Application @Inject extends Controller {

  /**
   * Serves the login page.
   */
  def login = Action { implicit request =>
    // Assign the user a UID (used to associate action logs with user sessions)
    val uid = request.session.get("uid").getOrElse { (Random.alphanumeric take 8).mkString }
    Logger.debug("Session UID: " + uid)
    // Show main page
    Ok(views.html.login()).withSession("uid" -> uid)
  }

  // TODO: comment for temporary alternate index-page
  // def index_alt = Action { implicit request => Ok(views.html.index_alt()) }

  /**
   * Serves the Networks of Names frontend to the client.
   */
  def index = Action { implicit request =>

    var authorized = false
    // TODO: commented out for disable auth
    // authorized = true

    // if we have an authorization code, we check it
    if (request.headers.toMap.contains("Authorization")) {
      var login = request.headers
        .get("Authorization")
        .toString
        .split(" ")(1)

      login = StringUtils.newStringUtf8(Base64.decodeBase64(login))

      if (login.matches("(.*):(.*)")) {
        val password = login.substring(login.indexOf(":") + 1)
        if (password == "shakti") {
          authorized = true
        }
      }
    }

    // now e check if authorization was successfull
    if (!authorized) {
      // send a login request
      Unauthorized(views.html.defaultpages.unauthorized())
        .withHeaders("WWW-Authenticate" -> "Basic realm=\"new/s/leak\"")
    } else {
      Ok(views.html.index())
    }
  }

  def jsRoutes(varName: String = "jsRoutes") = Action { implicit request =>
    // Note: feature warning is produced by play itself
    import play.api.routing._

    Ok(
      JavaScriptReverseRouter(varName)(
        // TODO: You need to add your routes here
        controllers.routes.javascript.DocumentController.getDocById,
        controllers.routes.javascript.DocumentController.getDocs,
        controllers.routes.javascript.DocumentController.getFrequencySeries,
        controllers.routes.javascript.DocumentController.getDocsByDate,
        controllers.routes.javascript.DocumentController.getDocsForMonth,
        controllers.routes.javascript.DocumentController.getDocsForYearRange,
        controllers.routes.javascript.NetworkController.getGraphData,
        controllers.routes.javascript.MapController.getDocsForCountry,
        controllers.routes.javascript.NetworkController.getEgoNetworkData,
        controllers.routes.javascript.NetworkController.getIdsByName,
        controllers.routes.javascript.NetworkController.deleteEntityById,
        controllers.routes.javascript.NetworkController.mergeEntitiesById,
        controllers.routes.javascript.NetworkController.changeEntityNameById,
        controllers.routes.javascript.NetworkController.getRelations,
        controllers.routes.javascript.EntityController.getEntities,
        controllers.routes.javascript.EntityController.getEntityTypes,
        controllers.routes.javascript.EntityController.getEntitiesByType,
        controllers.routes.javascript.EntityController.getEntitiesWithOffset,
        controllers.routes.javascript.EntityController.getEntitiesDocCount,
        controllers.routes.javascript.EntityController.getEntitiesDocCountWithOffset,
        controllers.routes.javascript.EntityController.getEntitiesDocCountWithFilter,
        controllers.routes.javascript.NetworkController.changeEntityTypeById,
        controllers.routes.javascript.MetadataController.getMetadata,
        controllers.routes.javascript.MetadataController.getSpecificMetadata,
        controllers.routes.javascript.MetadataController.getKeywords,
        controllers.routes.javascript.MetadataController.getMetadataTypes,
        controllers.routes.javascript.SearchController.getAutocomplete

      )
    ).as(JAVASCRIPT)
  }
}
