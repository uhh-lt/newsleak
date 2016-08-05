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

package util

import play.api.libs.json.{ JsArray, Writes }

/**
 * Created by flo on 8/1/2016.
 */
object TupleWriters {
  // http://stackoverflow.com/questions/30921821/play-scala-json-writer-for-seq-of-tuple

  implicit def tuple2Writes[A, B](implicit a: Writes[A], b: Writes[B]): Writes[(A, B)] = new Writes[(A, B)] {
    def writes(tuple: (A, B)) = JsArray(Seq(a.writes(tuple._1), b.writes(tuple._2)))
  }

  implicit def tuple3Writes[A, B, C](implicit a: Writes[A], b: Writes[B], c: Writes[C]): Writes[(A, B, C)] = new Writes[(A, B, C)] {
    def writes(tuple: (A, B, C)) = JsArray(Seq(
      a.writes(tuple._1),
      b.writes(tuple._2),
      c.writes(tuple._3)
    ))
  }

  implicit def tuple4Writes[A, B, C, D](implicit a: Writes[A], b: Writes[B], c: Writes[C], d: Writes[D]): Writes[(A, B, C, D)] = new Writes[(A, B, C, D)] {
    def writes(tuple: (A, B, C, D)) = JsArray(Seq(
      a.writes(tuple._1),
      b.writes(tuple._2),
      c.writes(tuple._3),
      d.writes(tuple._4)
    ))
  }

}
