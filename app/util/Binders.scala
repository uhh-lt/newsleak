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

import play.api.mvc.QueryStringBindable

/**
 * Created by flo on 6/19/2016.
 */
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
