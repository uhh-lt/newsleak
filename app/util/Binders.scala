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

import play.api.mvc.QueryStringBindable

/** Provides support for map types in play routes. */
object Binders {

  implicit def queryMapBinder(implicit stringBinder: QueryStringBindable[String]) = new QueryStringBindable[Map[String, String]] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Map[String, String]]] = {
      val data = for {
        (k, ps) <- params
        if k startsWith key
        p <- ps
      } yield (k.drop(key.length + 1), p)
      if (data.isEmpty) {
        None
      } else {
        Some(Right(data))
      }
    }

    override def unbind(key: String, map: Map[String, String]): String = {
      map.map(x => stringBinder.unbind(s"${key}.${x._1}", x._2)).foldLeft("")((a, b) => a + b + "&")
    }

    override def javascriptUnbind() =
      s"""function(k,v) {\n
          var res = "";\n
          v.forEach(function(entry) {\n
              res += encodeURIComponent(k+'.'+entry.key)+'='+entry.data+'&';\n
          });\n
          res.substr(0,res.length-1);\n
          return res;\n
        }"""
  }

  implicit def queryMapListBinder(implicit stringBinder: QueryStringBindable[String]) = new QueryStringBindable[Map[String, List[String]]] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Map[String, List[String]]]] = {
      val data = for {
        (k, ps) <- params
        if k startsWith key
        p <- ps
      } yield (k.drop(key.length + 1), p.split(',').toList)
      if (data.isEmpty) {
        None
      } else if (data.contains("dummy")) {
        Some(Right(Map()))
      } else {
        Some(Right(data))
      }
    }

    override def unbind(key: String, map: Map[String, List[String]]): String = {
      map.map(x => stringBinder.unbind(s"${key}.${x._1}", x._2.foldLeft("")((a, b) => a + b + ','))).foldLeft("")((a, b) => a + b + "&")
    }

    override def javascriptUnbind() =
      s"""function(k,v) {\n
          var res = "";\n
          v.forEach(function(entry) {\n
              var data = "";\n
              entry.data.forEach(function(x) {\n
                data += x + ',';\n
              });\n
              res += encodeURIComponent(k+'.'+entry.key)+'='+data.substr(0,data.length-1)+'&';\n
          });\n
          res.substr(0,res.length-1);\n
          return res;\n
        }"""
  }
}
