package com.comcast.money.emitters

import java.net._

import akka.actor._
import com.comcast.money.core.Money
import com.comcast.money.internal.EmitterProtocol.{EmitMetricLong, EmitMetricDouble}
import com.typesafe.config.Config
import org.joda.time.DateTimeUtils

object GraphiteMetricEmitter {

  lazy val localHostName = InetAddress.getLocalHost.getCanonicalHostName

  def props(conf: Config): Props = {
    Props(classOf[GraphiteMetricEmitter], conf)
  }
}

trait GraphiteNetworkAdapter {

  val graphiteAddress: InetAddress
  val graphitePort: Int

  def send(packet: DatagramPacket)

  def open()

  def close()
}

trait UDPGraphiteNetworkAdapter extends GraphiteNetworkAdapter {
  this: Configurable =>

  val graphiteAddress: InetAddress = InetAddress.getByName(conf.getString("hostname"))
  val graphitePort: Int = conf.getInt("port")

  var socketOption: Option[DatagramSocket] = None

  def send(packet: DatagramPacket) = socket.send(packet)

  def close() = socket.close()

  def open() = socketOption match {
    case None => newSocket()
    case _ => //we'll keep the socket we have
  }

  private def socket: DatagramSocket = socketOption match {
    case Some(s: DatagramSocket) if s.isClosed =>
      newSocket()
      socketOption.get
    case Some(s: DatagramSocket) => s
    case None => throw new IllegalStateException("Bad Socket")
  }

  private def newSocket() = socketOption = Some(new DatagramSocket())
}

class GraphiteMetricEmitter(val conf: Config)
  extends Actor with ActorLogging with Configurable with UDPGraphiteNetworkAdapter {

  private val GraphiteFormat: String = "%s %s %s\n"
  val appName = Money.applicationName

  def receive = {
    case EmitMetricDouble(metricPath: String, value: Double, timestamp: Long) =>
      val buffer = buildMessage(metricPath, value)
      log.debug("Emitting packet w/ {}, {}", metricPath, value)
      send(new DatagramPacket(buffer, buffer.length, graphiteAddress, graphitePort))
    case EmitMetricLong(metricPath: String, value: Long, timestamp: Long) =>
      val buffer = buildMessage(metricPath, value)
      log.debug("Emitting packet w/ {}, {}", metricPath, value)
      send(new DatagramPacket(buffer, buffer.length, graphiteAddress, graphitePort))
  }

  //XXX: probably more clever/scala-y way to do this
  def buildMessage(metricPath: String, value: Double): Array[Byte] = {
    import GraphiteMetricEmitter._
    val path: String = appName + '.' + localHostName.replace('.', '_') + '.' + metricPath
    val message: String = GraphiteFormat.format(path, value, DateTimeUtils.currentTimeMillis() / 1000L)
    log.debug(message)
    message.getBytes
  }

  def buildMessage(metricPath: String, value: Long): Array[Byte] = {
    import GraphiteMetricEmitter._
    val path: String = appName + '.' + localHostName.replace('.', '_') + '.' + metricPath
    val message: String = GraphiteFormat.format(path, value, DateTimeUtils.currentTimeMillis() / 1000L)
    log.debug(message)
    message.getBytes
  }

  override def preStart() = open()

  override def postStop() = close()
}
