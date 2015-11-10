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

package com.comcast.money.graphite

import java.net.InetSocketAddress

import akka.actor._
import akka.io.Udp._
import akka.io.{IO, Udp}
import com.comcast.money.emitters.LogRecord

object TestGraphiteServer {

  val actorSystem = ActorSystem("integration-test")
  val instance = actorSystem.actorOf(Props(classOf[TestGraphiteServer], 2003), "graphite-server")
}

class TestGraphiteServer(port: Int) extends Actor with ActorLogging {

  val udpManager = IO(Udp)(context.system)
  val messageHandler = context.actorOf(Props[TestGraphiteServerHandler], "graphite-server-handler")
  var udpWorker: ActorRef = _

  override def preStart() = {
    udpManager ! Bind(messageHandler, new InetSocketAddress("localhost", port))
  }

  def receive = {
    case e: Send => udpWorker forward e
    case Bound(localAddress) =>
      log.warning(s"GraphiteServerHandler bound to $localAddress")
      udpWorker = sender
    case Unbound =>
      log.warning("Unbound")
      udpWorker = null
  }
}

class TestGraphiteServerHandler extends Actor with ActorLogging {

  def receive = {
    case Received(data, remoteAddress) =>
      val msg = data.utf8String
      log.warning(s"Received message '$msg' from address $remoteAddress")
      LogRecord.add("graphite", msg)
  }
}
