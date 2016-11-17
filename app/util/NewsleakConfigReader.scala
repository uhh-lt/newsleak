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

import java.io.File

import com.typesafe.config.{ Config, ConfigFactory }
import scalikejdbc.config.{ DBs, NoEnvPrefix, StandardTypesafeConfig, TypesafeConfigReader }

// scalastyle:off
import scala.collection.JavaConverters._
// scalastyle:on

case class ESSettings(clusterName: String, address: String, port: Int)

/**
 * Handles the initialization of the connections defined in the
 * conf/application.conf file and provides parameter from it.
 */
object NewsleakConfigReader extends DBs
    with TypesafeConfigReader
    with StandardTypesafeConfig
    with NoEnvPrefix {

  override lazy val config: Config = ConfigFactory.parseFile(new File("conf/application.conf"))

  lazy val esDefaultIndex: String = config.getString("es.index.default")

  lazy val esIndices: List[String] = config.getStringList("es.indices").asScala.toList

  lazy val esSettings: ESSettings = {
    val clusterName = NewsleakConfigReader.config.getString("es.clustername")
    val address = NewsleakConfigReader.config.getString("es.address")
    val port = NewsleakConfigReader.config.getInt("es.port")
    ESSettings(clusterName, address, port)
  }

  lazy val excludedMetadataTypes: Map[String, List[String]] = {
    esIndices.map { index => index -> config.getStringList(s"es.$index.excludeTypes").asScala.toList }.toMap
  }
}
