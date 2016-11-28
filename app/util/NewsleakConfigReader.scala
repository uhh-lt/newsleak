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
import scalikejdbc.config.{ NoEnvPrefix, StandardTypesafeConfig, TypesafeConfigReader }

// scalastyle:off
import scala.collection.JavaConverters._
// scalastyle:on

/**
 * Representation for the elasticsearch connection information.
 *
 * @param clusterName the elasticsearch cluster name.
 * @param address the elasticsearch transport address.
 * @param port the elasticsearch transport address port.
 */
case class ESSettings(clusterName: String, address: String, port: Int)

/** Provides access to the settings defined in the ''conf/application.conf'' file. */
object NewsleakConfigReader extends TypesafeConfigReader
    with StandardTypesafeConfig
    with NoEnvPrefix {

  /** Provides all settings defined in the ''conf/application.conf'' file. */
  override lazy val config: Config = ConfigFactory.parseFile(new File("conf/application.conf"))
  /** Provides the default elasticsearch index used for setting the default collection. */
  lazy val esDefaultIndex: String = config.getString("es.index.default")
  /** Provides the available elasticsearch indices. */
  lazy val esIndices: List[String] = config.getStringList("es.indices").asScala.toList
  /** Provides the elasticsearch connection information. */
  lazy val esSettings: ESSettings = {
    val clusterName = NewsleakConfigReader.config.getString("es.clustername")
    val address = NewsleakConfigReader.config.getString("es.address")
    val port = NewsleakConfigReader.config.getInt("es.port")
    ESSettings(clusterName, address, port)
  }

  /** Provides the metadata keys that should be excluded from the frequency charts. */
  lazy val excludedMetadataTypes: Map[String, List[String]] = {
    esIndices.map { index => index -> config.getStringList(s"es.$index.excludeTypes").asScala.toList }.toMap
  }
}
