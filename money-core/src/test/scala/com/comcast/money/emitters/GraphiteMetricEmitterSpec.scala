package com.comcast.money.emitters

import java.net.DatagramPacket

import akka.actor.{ActorKilledException, Kill}
import akka.testkit.{EventFilter, TestActorRef}
import com.comcast.money.internal.EmitterProtocol.{EmitMetricDouble, EmitMetricLong}
import com.comcast.money.test.AkkaTestJawn
import com.typesafe.config.Config
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeUtils.MillisProvider
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, OneInstancePerTest, WordSpecLike}

import scala.collection.mutable

trait MockGraphiteNetworkAdapter extends GraphiteNetworkAdapter with MockitoSugar {

  val packets = mutable.MutableList[DatagramPacket]()
  var isOpen: Boolean = false

  var sendImpl: (DatagramPacket) => Unit = (packet) => packets += packet

  override def send(packet: DatagramPacket) = {
    sendImpl(packet)
  }

  override def close() = isOpen = false

  override def open() = isOpen = true
}

class GraphiteMetricEmitterSpec
  extends AkkaTestJawn with WordSpecLike with MockitoSugar with OneInstancePerTest with BeforeAndAfterEach {

  val conf: Config = mock[Config]

  val emitter = TestActorRef(new GraphiteMetricEmitter(conf) with MockGraphiteNetworkAdapter)

  override def beforeEach = {
    DateTimeUtils.setCurrentMillisProvider(
      new MillisProvider {
        override def getMillis: Long = 2000L
      })
  }

  override def afterEach = DateTimeUtils.setCurrentMillisSystem()

  "A GraphiteMetricEmitter must" should {
    "open a connection after it is started" in {
      emitter ! EmitMetricDouble("path", 1.0)

      awaitCond(emitter.underlyingActor.isOpen)
    }
    "close a connection when it is killed" in {
      val actor = emitter.underlyingActor
      EventFilter[ActorKilledException](occurrences = 1) intercept {
        emitter ! Kill
      }
      assert(!actor.isOpen)
    }
    "emit to Graphite when receiving EMIT message" in {
      val actor = emitter.underlyingActor
      emitter ! EmitMetricDouble("path", 1.0)
      awaitCond(actor.isOpen)
      awaitCond(actor.packets.size == 1)
      awaitCond(new String(actor.packets(0).getData).endsWith("path 1.0 2\n"))
    }
    "emit longs to Graphite when receiving EMIT message" in {
      val actor = emitter.underlyingActor
      emitter ! EmitMetricLong("path", 20L)
      awaitCond(actor.isOpen)
      awaitCond(actor.packets.size == 1)
      awaitCond(new String(actor.packets(0).getData).endsWith("path 20 2\n"))
    }
    "rethrow an exception if one is thrown while sending a message" in {
      emitter.underlyingActor.sendImpl = (packet) => {
        throw new IllegalStateException("bad boy")
      }

      EventFilter[IllegalStateException](occurrences = 1) intercept {
        emitter ! EmitMetricDouble("path", 1.0)
      }
    }
    "divide the timestamp by 1000 so the timestamp is in seconds and not microseconds" in {
      val actor = emitter.underlyingActor
      emitter ! EmitMetricDouble("path", 1.0)
      awaitCond(actor.packets.size == 1)
      awaitCond(new String(actor.packets(0).getData).endsWith("path 1.0 2\n"))
    }
  }
}
