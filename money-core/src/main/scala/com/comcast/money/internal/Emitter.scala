package com.comcast.money.internal

import akka.actor.{Actor, ActorLogging, Props}
import com.comcast.money.akka.ActorMaker
import com.comcast.money.core.{Money, Span}
import com.comcast.money.internal.EmitterBus._
import com.comcast.money.internal.EmitterProtocol.{EmitMetricLong, EmitMetricDouble, EmitSpan}
import com.comcast.money.util.DateTimeUtil
import com.typesafe.config.Config

object EmitterProtocol {

  final case class EmitSpan(t: Span) extends EmitData
  final case class EmitMetricDouble(metricPath: String, value: Double, timestamp: Long = DateTimeUtil.microTime)
    extends EmitData
  final case class EmitMetricLong(metricPath: String, value: Long, timestamp: Long = DateTimeUtil.microTime)
    extends EmitData
}

object Emitter {

  def props(): Props = {
    Props(classOf[Emitter], new EmitterBus())
  }
}

class Emitter(emitterBus: EmitterBus) extends Actor with ActorMaker with ActorLogging {
  val emitterConfig = Money.config.getConfig("money.emitter")

  override def preStart(): Unit = {
    import scala.collection.JavaConversions._
    val emitterConfs: List[_ <: Config] = emitterConfig.getConfigList("emitters").toList

    def registerEmitter(conf: Config) {
      val name = conf.getString("name")
      val className = conf.getString("class-name")
      val configuration = conf.getConfig("configuration")
      val subscriptions = conf.getStringList("subscriptions")
      log.info("Registering emitter: {} ", name)
      log.debug("Emitter registration: evaluating {} ", configuration.toString)
      val clazz = Class.forName(className)
      val props = Props(clazz, configuration)
      val emitter = makeActor(props, name)
      if (subscriptions.isEmpty) {
        emitterBus.subscribe(emitter, Trace)
        emitterBus.subscribe(emitter, Metric)
      } else {
        subscriptions.foreach {
          case "Trace" => emitterBus.subscribe(emitter, Trace)
          case "Metric" => emitterBus.subscribe(emitter, Metric)
          case unknownSubscription: String => throw new
              IllegalStateException(s"Unknown subscription: $unknownSubscription")
        }
      }
    }
    emitterConfs.foreach(registerEmitter)
  }

  def receive = {
    case span: EmitSpan =>
      emitterBus.publish(EmitterEvent(Trace, span))

    case metricDouble: EmitMetricDouble =>
      emitterBus.publish(EmitterEvent(Metric, metricDouble))

    case metricLong: EmitMetricLong =>
      emitterBus.publish(EmitterEvent(Metric, metricLong))
  }
}

