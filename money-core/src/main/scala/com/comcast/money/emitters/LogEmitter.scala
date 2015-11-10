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

import akka.actor.{ Actor, ActorLogging, Props }
import com.comcast.money.core._
import com.typesafe.config.Config
import org.slf4j.MDC

object LogEmitter {

  val logTemplate = "[ %s=%s ]"
  val NULL = "NULL"

  def props(conf: Config): Props = {
    val logEmitterClass = conf.getString("emitter")
    Props(Class.forName(logEmitterClass), conf)
  }

  def buildMessage(t: Span): String = {
    implicit val builder = new StringBuilder()
    builder.append("Span: ")
    append("span-id", t.spanId.spanId)
    append("trace-id", t.spanId.traceId)
    append("parent-id", t.spanId.parentSpanId)
    append("span-name", t.spanName)
    append("app-name", Money.applicationName)
    append("start-time", t.startTime)
    append("span-duration", t.duration)
    append("span-success", t.success)
    t.notes.toList.sortBy(_._1).foreach {
      case (name, note) => note match {
        case n: Note[_] if n.value.isEmpty => append(n.name, NULL)
        case n: Note[_] if n.value.isDefined => append(n.name, n.value.get.toString)
      }
    }
    builder.toString()
  }

  private def append[T](key: String, value: T)(implicit builder: StringBuilder): StringBuilder = builder
    .append(logTemplate.format(key, value))
}

import com.comcast.money.internal.EmitterProtocol._

class LogEmitter(val conf: Config) extends Actor with ActorLogging with Configurable {
  def receive = {
    case EmitSpan(t: Span) =>
      record(LogEmitter.buildMessage(t))
    case metric: EmitMetricDouble =>
      record(s"${metric.metricPath}=${metric.value}")
  }

  def record(message: String) = {
    MDC.clear()
    log.warning(message)
  }
}
