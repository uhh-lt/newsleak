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
