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

package models.services

import java.net.InetAddress
import javax.inject.Singleton

import com.google.inject.ImplementedBy
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import util.NewsleakConfigReader

/**
 * Wrapper around an ElasticSearch [[Client]]. Different ways of obtaining a [[Client]] can be implemented in classes
 * implementing this interface.
 */
@ImplementedBy(classOf[ESTransportClient])
trait SearchClientService {
  /** Get a reference to an ElasticSearch [[Client]]. */
  def client(): Client

  /** Shutdown the ElasticSearch [[Client]]. The client is not available for querying and indexing. */
  def shutdown(): Unit
}

/** Wrapper around an ElasticSearch [[TransportClient]] node. */
@Singleton
class ESTransportClient extends SearchClientService {

  private val settings = NewsleakConfigReader.esSettings

  private val transportSettings = Settings.settingsBuilder()
    .put("cluster.name", settings.clusterName).build()

  private lazy val transportClient = TransportClient.builder().settings(transportSettings).build()
    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(settings.address), settings.port))

  override def client: Client = transportClient

  override def shutdown(): Unit = {
    client.close()
  }
}
