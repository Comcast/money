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

package com.comcast.money.core.handlers

import com.comcast.money.api.{ SpanHandler, SpanInfo }
import com.typesafe.config.Config
import org.slf4j.{ Logger, LoggerFactory }

import scala.collection.JavaConversions._

object LoggingSpanHandler {

  type LogFunction = String => Unit

  val HEADER_FORMAT: String = "Span: [ span-id=%s ][ trace-id=%s ][ parent-id=%s ][ span-name=%s ][ " +
    "app-name=%s ][ start-time=%s ][ span-duration=%s ][ span-success=%s ] [ host=%s ]"
  val NOTE_BEGIN = "[ "
  val NOTE_END = " ]"
  val EQ = "="
  val NULL: String = "NULL"
}

class LoggingSpanHandler(val logger: Logger) extends ConfigurableHandler {

  def this() = this(LoggerFactory.getLogger(classOf[LoggingSpanHandler]))

  import LoggingSpanHandler._

  protected var logFunction: LogFunction = logger.info

  def configure(config: Config): Unit = {

    if (config.hasPath("log-level")) {
      val level = config.getString("log-level").toUpperCase

      // set the log level based on the configured value
      level match {
        case "ERROR" => logFunction = logger.error
        case "WARN" => logFunction = logger.warn
        case "INFO" => logFunction = logger.info
        case "DEBUG" => logFunction = logger.debug
        case "TRACE" => logFunction = logger.trace
      }
    }
  }

  def handle(spanInfo: SpanInfo): Unit = {

    val sb: StringBuilder = new StringBuilder
    sb.append(
      HEADER_FORMAT.format(
        spanInfo.id.selfId, spanInfo.id.traceId, spanInfo.id.parentId, spanInfo.name, spanInfo.appName,
        spanInfo.startTimeMillis, spanInfo.durationMicros, spanInfo.success, spanInfo.host
      )
    )

    for (note <- spanInfo.notes.values) {
      sb.append(NOTE_BEGIN)
        .append(note.name)
        .append(EQ)
        .append(valueOrNull(note.value))
        .append(NOTE_END)
    }

    logFunction(sb.toString)
  }

  private def valueOrNull[T](value: T) =
    if (value == null)
      NULL
    else
      value
}

