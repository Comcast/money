/*
 * Copyright 2012-2015 Comcast Cable Communications Management, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comcast.money.kafka

import kafka.utils.Logging
import org.apache.curator.test.TestingServer

/**
 * Runs an in-memory, "embedded" instance of a ZooKeeper server.
 *
 * The ZooKeeper server instance is automatically started when you create a new instance of this class.
 *
 * @param port The port (aka `clientPort`) to listen to.  Default: 2181.
 */
class ZooKeeperEmbedded(val port: Int = 2181) extends Logging {

  debug(s"Starting embedded ZooKeeper server on port ${port}...")

  private val server = new TestingServer(port)

  /**
   * Stop the instance.
   */
  def stop() {
    debug("Shutting down embedded ZooKeeper server...")
    server.close()
    debug("Embedded ZooKeeper server shutdown completed")
  }

  /**
   * The ZooKeeper connection string aka `zookeeper.connect` in `hostnameOrIp:port` format.
   * Example: `127.0.0.1:2181`.
   *
   * You can use this to e.g. tell Kafka and Storm how to connect to this instance.
   */
  val connectString: String = server.getConnectString

  /**
   * The hostname of the ZooKeeper instance.  Example: `127.0.0.1`
   */
  val hostname: String = connectString.splitAt(connectString lastIndexOf ':')._1 // "foo:1:2:3" -> ("foo:1:2", ":3)

}
