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

import play.api.libs.json.{ JsArray, Writes }

/**
 * Definitions to convert tuple, triple and 4-tuple to [[JsArray]].
 *
 * @see taken from [[http://stackoverflow.com/questions/30921821/play-scala-json-writer-for-seq-of-tuple]].
 */
object TupleWriters {

  /** Converts tuple to [[JsArray]] instances. */
  implicit def tuple2Writes[A, B](implicit a: Writes[A], b: Writes[B]): Writes[(A, B)] = new Writes[(A, B)] {
    def writes(tuple: (A, B)) = JsArray(Seq(a.writes(tuple._1), b.writes(tuple._2)))
  }

  /** Converts triple to [[JsArray]] instances. */
  implicit def tuple3Writes[A, B, C](implicit a: Writes[A], b: Writes[B], c: Writes[C]): Writes[(A, B, C)] = new Writes[(A, B, C)] {
    def writes(tuple: (A, B, C)) = JsArray(Seq(
      a.writes(tuple._1),
      b.writes(tuple._2),
      c.writes(tuple._3)
    ))
  }

  /** Converts 4-tuple to [[JsArray]] instances. */
  implicit def tuple4Writes[A, B, C, D](implicit a: Writes[A], b: Writes[B], c: Writes[C], d: Writes[D]): Writes[(A, B, C, D)] = new Writes[(A, B, C, D)] {
    def writes(tuple: (A, B, C, D)) = JsArray(Seq(
      a.writes(tuple._1),
      b.writes(tuple._2),
      c.writes(tuple._3),
      d.writes(tuple._4)
    ))
  }
}
