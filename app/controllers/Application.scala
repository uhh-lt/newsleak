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

import play.api.Logger
import play.api.mvc.{ Action, Controller }
import org.apache.commons.codec.binary.{ Base64, Hex, StringUtils }
import javax.inject.Inject

import model.faceted.search.FacetedSearch

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
    val uid = request.session.get("uid").getOrElse { (Random.alphanumeric take 8).mkString }
    Logger.debug("Session UID: " + uid)

    var authorized = false
    // TODO: commented out for disable auth
    authorized = true

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
      Ok(views.html.index()).withSession(
        "uid" -> uid
      )
    }
  }

  def switchDataset(dataSet: String) = Action {
    FacetedSearch.changeIndex(dataSet)
    utils.DBService.changeDB(dataSet)
    Ok("success").as("Text")
  }

  def jsRoutes(varName: String = "jsRoutes") = Action { implicit request =>
    // Note: feature warning is produced by play itself
    import play.api.routing._

    Ok(
      JavaScriptReverseRouter(varName)(
        // TODO: You need to add your routes here
        controllers.routes.javascript.DocumentController.getDocById,
        controllers.routes.javascript.DocumentController.getDocs,
        controllers.routes.javascript.NetworkController.getIdsByName,
        controllers.routes.javascript.NetworkController.deleteEntityById,
        controllers.routes.javascript.NetworkController.mergeEntitiesById,
        controllers.routes.javascript.NetworkController.changeEntityNameById,
        controllers.routes.javascript.NetworkController.induceSubgraph,
        controllers.routes.javascript.EntityController.getEntities,
        controllers.routes.javascript.EntityController.getEntityTypes,
        controllers.routes.javascript.EntityController.getEntitiesByType,
        controllers.routes.javascript.NetworkController.changeEntityTypeById,
        controllers.routes.javascript.MetadataController.getMetadata,
        controllers.routes.javascript.MetadataController.getSpecificMetadata,
        controllers.routes.javascript.MetadataController.getKeywords,
        controllers.routes.javascript.MetadataController.getMetadataTypes,
        controllers.routes.javascript.SearchController.getAutocomplete,
        controllers.routes.javascript.HistogramController.getHistogram,
        controllers.routes.javascript.HistogramController.getHistogramLod

      )
    ).as(JAVASCRIPT)
  }
}
