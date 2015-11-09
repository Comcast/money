package com.comcast.money.emitters

import akka.actor._
import akka.routing.RoundRobinRouter
import com.comcast.money.akka.ActorMaker
import com.comcast.money.core.Span
import com.comcast.money.internal.EmitterProtocol.{EmitMetricDouble, EmitMetricLong, EmitSpan}
import com.typesafe.config.Config

object GraphiteEmitter {

  def props(conf: Config): Props = {

    Props(classOf[GraphiteEmitter], conf)
  }
}

class GraphiteEmitter(conf:Config) extends Actor with ActorMaker with ActorLogging {

  private val GraphiteTracingDataPath: String = "request-tracing-data"

  private val emitterPoolSize = conf.getInt("emitterPoolSize")
  private val router = makeActor(GraphiteMetricEmitter.props(conf).withRouter(RoundRobinRouter(nrOfInstances = emitterPoolSize)), "graphite-router")

  def receive = {

    case e: EmitMetricDouble => router forward e
    case EmitSpan(t: Span) =>
      router ! EmitMetricLong(genPath(t.spanName, "span-duration", GraphiteTracingDataPath), t.duration, t.startTime)
      for ((name, note) <- t.notes) {
        note.value match {
          case None =>
            log.debug("Emitting to Graphite timing {}, {}", name, note.timestamp)
            router ! EmitMetricLong(genPath(t.spanName, name, GraphiteTracingDataPath), note.timestamp, note.timestamp)
          case Some(value:Double) =>
            log.debug("Emitting to Graphite Data {}, {}", name, value)
            router ! EmitMetricDouble(genPath(t.spanName, note.name, GraphiteTracingDataPath), value, note.timestamp)
          case Some(value:Long) =>
            log.debug("Emitting to Graphite Data {}, {}", name, value)
            router ! EmitMetricLong(genPath(t.spanName, note.name, GraphiteTracingDataPath), value, note.timestamp)
          case _ => //not a timing and data is not assignable to Double so just dropping it for now
        }
      }
  }

  private def genPath(recordKey: String, measurementKey: String, path: String) = path + "." + recordKey.replace('.', '_').replace(" ", "_") + "." + measurementKey.replace('.', '_').replace(" ", "_")
}
