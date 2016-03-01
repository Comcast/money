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
import akka.event.Logging
import akka.event.Logging.LogLevel
import com.comcast.money.core._
import com.typesafe.config.Config
import org.slf4j.MDC

import scala.util.Try

object LogEmitter {

  def props(conf: Config): Props =
    Try {
      val emitter = conf.getString("emitter")
      Props(Class.forName(emitter), conf)
    }.recover {
      case _ => Props(classOf[LogEmitter], conf)
    }.get

  def logLevel(conf: Config): LogLevel =
    if (conf.hasPath("log-level"))
      Logging.levelFor(conf.getString("log-level")).getOrElse(Logging.WarningLevel)
    else
      Logging.WarningLevel
}

import com.comcast.money.internal.EmitterProtocol._

class LogEmitter(val conf: Config) extends Actor with ActorLogging with Configurable {

  private val spanLogFormatter = SpanLogFormatter(conf)
  private val level = LogEmitter.logLevel(conf)

  def receive = {
    case EmitSpan(t: Span) =>
      record(spanLogFormatter.buildMessage(t))
    case metric: EmitMetricDouble =>
      record(s"${metric.metricPath}=${metric.value}")
  }

  def record(message: String) = {
    MDC.clear()
    log.log(level, message)
  }
}
