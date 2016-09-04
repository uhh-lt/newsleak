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

import play.api.mvc.{ Action, Results, Controller }
import model.Entity
import play.api.libs.json.Json
import javax.inject.Inject

// scalastyle:off
import util.TupleWriters._
// scalastyle:on

/**
 * Created by patrick on 19.04.16.
 */
class SearchController @Inject extends Controller {

  /**
   * get the autocomplete tags to this query
   *
   * @param query the query to get the autocomplete
   *              tags for
   * @return
   *         an array of entity names and entity types
   *         combined
   */
  def getAutocomplete(query: String) = Action {
    Results.Ok(Json.obj("entities" -> Entity.getByNamePattern(query)
      .map(entity => (entity.id, entity.name, entity.entityType.toString)))).as("application/json")
  }
}
