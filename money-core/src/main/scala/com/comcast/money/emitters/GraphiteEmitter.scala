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

package com.comcast.money.emitters

import akka.actor._
import akka.routing.RoundRobinRouter
import com.comcast.money.akka.ActorMaker
import com.comcast.money.core.Span
import com.comcast.money.internal.EmitterProtocol.{ EmitMetricDouble, EmitMetricLong, EmitSpan }
import com.typesafe.config.Config

object GraphiteEmitter {

  def props(conf: Config): Props = {

    Props(classOf[GraphiteEmitter], conf)
  }
}

class GraphiteEmitter(conf: Config) extends Actor with ActorMaker with ActorLogging {

  private val GraphiteTracingDataPath: String = "request-tracing-data"

  private val emitterPoolSize = conf.getInt("emitterPoolSize")
  private val router = makeActor(
    GraphiteMetricEmitter.props(conf).withRouter(RoundRobinRouter(nrOfInstances = emitterPoolSize)), "graphite-router"
  )

  def receive = {

    case e: EmitMetricDouble => router forward e
    case EmitSpan(t: Span) =>
      router ! EmitMetricLong(genPath(t.spanName, "span-duration", GraphiteTracingDataPath), t.duration, t.startTime)
      for ((name, note) <- t.notes) {
        note.value match {
          case None =>
            log.debug("Emitting to Graphite timing {}, {}", name, note.timestamp)
            router ! EmitMetricLong(genPath(t.spanName, name, GraphiteTracingDataPath), note.timestamp, note.timestamp)
          case Some(value: Double) =>
            log.debug("Emitting to Graphite Data {}, {}", name, value)
            router ! EmitMetricDouble(genPath(t.spanName, note.name, GraphiteTracingDataPath), value, note.timestamp)
          case Some(value: Long) =>
            log.debug("Emitting to Graphite Data {}, {}", name, value)
            router ! EmitMetricLong(genPath(t.spanName, note.name, GraphiteTracingDataPath), value, note.timestamp)
          case _ => //not a timing and data is not assignable to Double so just dropping it for now
        }
      }
  }

  private def genPath(recordKey: String, measurementKey: String, path: String) = path + "." + recordKey
    .replace('.', '_').replace(" ", "_") + "." + measurementKey.replace('.', '_').replace(" ", "_")
}
