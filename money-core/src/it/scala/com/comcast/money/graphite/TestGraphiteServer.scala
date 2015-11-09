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
