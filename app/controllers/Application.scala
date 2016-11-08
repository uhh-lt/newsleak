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

import org.apache.commons.codec.binary.{ Base64, StringUtils }
import play.api.Logger
import play.api.cache.CacheApi
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }
import util.SessionUtils.{ currentDataset, datasetSessionKey }
import utils.NewsleakConfigReader

import scala.util.Random

/*
  This class define which URL patterns match which views as well as
  which play routes exist and where they point at.
*/
class Application @Inject() (cache: CacheApi) extends Controller {

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
   * Serves the application frontend to the client.
   */
  def index = Action { implicit request =>
    val uid = request.session.get("uid").getOrElse { (Random.alphanumeric take 8).mkString }
    Logger.debug("Session UID: " + uid)
    // The application expects having an empty cache after page reload
    cache.remove(uid)

    var authorized = false
    if (NewsleakConfigReader.config.getBoolean("authorization.enabled")) {
      authorized = true
    }
    // if we have an authorization code, we check it
    if (request.headers.toMap.contains("Authorization")) {
      var login = request.headers
        .get("Authorization")
        .toString
        .split(" ")(1)

      login = StringUtils.newStringUtf8(Base64.decodeBase64(login))

      if (login.matches("(.*):(.*)")) {
        val password = login.substring(login.indexOf(":") + 1)
        if (password == NewsleakConfigReader.config.getString("authorization.password")) {
          authorized = true
        }
      }
    }

    // now check if authorization was successful
    if (!authorized) {
      // send a login request
      Unauthorized(views.html.defaultpages.unauthorized())
        .withHeaders("WWW-Authenticate" -> "Basic realm=\"new/s/leak\"")
    } else {
      Ok(views.html.index()).withNewSession.withSession(
        "uid" -> uid,
        // Initialize application with default ES index dataset
        datasetSessionKey -> NewsleakConfigReader.config.getString("es.index.default")
      )
    }
  }

  def getDatasets() = Action { implicit request =>
    Ok(Json.obj("current" -> NewsleakConfigReader.esDefaultIndex, "available" -> NewsleakConfigReader.dbNames))
  }

  def changeDataset(name: String) = Action { implicit request =>
    Ok(Json.obj("newDataset" -> name, "oldDataset" -> currentDataset))
      .withSession(request.session + (datasetSessionKey, name))
  }

  // scalastyle:off
  def jsRoutes(varName: String = "jsRoutes") = Action { implicit request =>
    // Note: feature warning is produced by play itself
    import play.api.routing._
    // scalastyle:on

    Ok(
      JavaScriptReverseRouter(varName)(
        // TODO: You need to add your routes here
        controllers.routes.javascript.Application.getDatasets,
        controllers.routes.javascript.Application.changeDataset,
        controllers.routes.javascript.DocumentController.getDocById,
        controllers.routes.javascript.DocumentController.getKeywordsById,
        controllers.routes.javascript.DocumentController.getDocs,
        controllers.routes.javascript.DocumentController.addTag,
        controllers.routes.javascript.DocumentController.removeTagById,
        controllers.routes.javascript.DocumentController.getTagsByDocId,
        controllers.routes.javascript.DocumentController.getTagLabels,
        controllers.routes.javascript.DocumentController.getDocsByLabel,
        controllers.routes.javascript.NetworkController.getIdsByName,
        controllers.routes.javascript.NetworkController.blacklistEntitiesById,
        controllers.routes.javascript.NetworkController.mergeEntitiesById,
        controllers.routes.javascript.NetworkController.changeEntityNameById,
        controllers.routes.javascript.NetworkController.induceSubgraph,
        controllers.routes.javascript.NetworkController.addNodes,
        controllers.routes.javascript.NetworkController.getEdgeKeywords,
        controllers.routes.javascript.NetworkController.getNeighborCounts,
        controllers.routes.javascript.NetworkController.getNeighbors,
        controllers.routes.javascript.EntityController.getEntities,
        controllers.routes.javascript.EntityController.getEntityTypes,
        controllers.routes.javascript.EntityController.getEntitiesByType,
        controllers.routes.javascript.EntityController.getBlacklistedEntities,
        controllers.routes.javascript.EntityController.getMergedEntities,
        controllers.routes.javascript.EntityController.undoBlacklistingByIds,
        controllers.routes.javascript.EntityController.undoMergeByIds,
        controllers.routes.javascript.EntityController.getEntitiesByDoc,
        controllers.routes.javascript.NetworkController.changeEntityTypeById,
        controllers.routes.javascript.MetadataController.getMetadata,
        controllers.routes.javascript.MetadataController.getSpecificMetadata,
        controllers.routes.javascript.MetadataController.getKeywords,
        controllers.routes.javascript.MetadataController.getMetadataTypes,
        controllers.routes.javascript.HistogramController.getHistogram,
        controllers.routes.javascript.HistogramController.getXHistogram,
        controllers.routes.javascript.HistogramController.getHistogramLod
      )
    ).as(JAVASCRIPT)
  }
}
