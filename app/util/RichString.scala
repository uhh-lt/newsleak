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

import scala.util.control.Exception.catching

/**
 * Implements additional methods for [[String]].
 *
 * @param underlying the wrapped string.
 */
class RichString(underlying: String) {

  def toLongOpt(): Option[Long] = catching(classOf[NumberFormatException]) opt underlying.toInt
}

/**
 * Companion object for [[RichString]] that provides a convenient method
 * to wrap a instance of [[String]].
 */
object RichString {

  implicit def richString(string: String): RichString = new RichString(string)
}
