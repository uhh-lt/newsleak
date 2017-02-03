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
import util.NewsleakConfigReader
import util.SessionUtils.{ currentDataset, datasetSessionKey }

import scala.util.Random

/**
 * Provides the application view, handles the login, dataset change and the javascript reverse routes.
 *
 * @param cache the application cache.
 */
class Application @Inject() (cache: CacheApi) extends Controller {

  private val uidLength = 8
  private def generateUid: String = Random.alphanumeric.take(uidLength).mkString

  /** Serves the application frontend to the client. */
  def index = Action { implicit request =>
    val uid = request.session.get("uid").getOrElse(generateUid)
    Logger.debug("Session UID: " + uid)
    // The application expects an empty cache after page reload
    cache.remove(uid)

    var authorized = false
    // Set valid authorization if disabled
    if (!NewsleakConfigReader.config.getBoolean("authorization.enabled")) {
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

    // Check if authorization was successful
    if (!authorized) {
      // send a login request
      Unauthorized(views.html.defaultpages.unauthorized())
        .withHeaders("WWW-Authenticate" -> "Basic realm=\"new/s/leak\"")
    } else {
      Ok(views.html.index()).withNewSession.withSession(
        "uid" -> uid,
        // Initialize application with the default ES index dataset
        datasetSessionKey -> NewsleakConfigReader.config.getString("es.index.default")
      )
    }
  }

  /** Returns the current applied dataset and all available datasets. */
  def getDatasets() = Action { implicit request =>
    Ok(Json.obj("current" -> NewsleakConfigReader.esDefaultIndex, "available" -> NewsleakConfigReader.dbNames))
  }

  /** Issues a dataset switch. */
  def changeDataset(name: String) = Action { implicit request =>
    Ok(Json.obj("newDataset" -> name, "oldDataset" -> currentDataset))
      .withSession(request.session + (datasetSessionKey, name))
  }

  // scalastyle:off
  /** Returns the application route for the given call. */
  def jsRoutes(varName: String = "jsRoutes") = Action { implicit request =>
    // Note: feature warning is produced by play itself
    import play.api.routing._
    // scalastyle:on

    Ok(
      JavaScriptReverseRouter(varName)(
        controllers.routes.javascript.Application.getDatasets,
        controllers.routes.javascript.Application.changeDataset,
        controllers.routes.javascript.DocumentController.getKeywordsById,
        controllers.routes.javascript.DocumentController.getDocs,
        controllers.routes.javascript.DocumentController.getDocsByIds,
        controllers.routes.javascript.DocumentController.addTag,
        controllers.routes.javascript.DocumentController.removeTagById,
        controllers.routes.javascript.DocumentController.getTagsByDocId,
        controllers.routes.javascript.DocumentController.getTagLabels,
        controllers.routes.javascript.DocumentController.getDocsByLabel,
        controllers.routes.javascript.NetworkController.blacklistEntitiesById,
        controllers.routes.javascript.NetworkController.mergeEntitiesById,
        controllers.routes.javascript.NetworkController.changeEntityNameById,
        controllers.routes.javascript.NetworkController.induceSubgraph,
        controllers.routes.javascript.NetworkController.addNodes,
        controllers.routes.javascript.NetworkController.getEdgeKeywords,
        controllers.routes.javascript.NetworkController.getNeighborCounts,
        controllers.routes.javascript.NetworkController.getNeighbors,
        controllers.routes.javascript.EntityController.getEntitiesByType,
        controllers.routes.javascript.EntityController.getEntityTypes,
        controllers.routes.javascript.EntityController.getBlacklistedEntities,
        controllers.routes.javascript.EntityController.getMergedEntities,
        controllers.routes.javascript.EntityController.undoBlacklistingByIds,
        controllers.routes.javascript.EntityController.undoMergeByIds,
        controllers.routes.javascript.EntityController.getEntitiesByDoc,
        controllers.routes.javascript.NetworkController.changeEntityTypeById,
        controllers.routes.javascript.MetadataController.getMetadata,
        controllers.routes.javascript.MetadataController.getSpecificMetadata,
        controllers.routes.javascript.MetadataController.getMetadataTypes,
        controllers.routes.javascript.HistogramController.getTimeline,
        controllers.routes.javascript.HistogramController.getTimeExprTimeline,
        controllers.routes.javascript.HistogramController.getTimelineLOD
      )
    ).as(JAVASCRIPT)
  }
}
