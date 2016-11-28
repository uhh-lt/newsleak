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

package util

import play.api.mvc.{ AnyContent, Request }

/** Provides information from the current user session. */
object SessionUtils {

  /** Session key for the name of the current user selected dataset. */
  val datasetSessionKey = "dataset"

  /** Provides the name for the current selected dataset. Only works in an environment where a user request is available. */
  def currentDataset(implicit request: Request[AnyContent]): String = request.session.get(datasetSessionKey).get
}
