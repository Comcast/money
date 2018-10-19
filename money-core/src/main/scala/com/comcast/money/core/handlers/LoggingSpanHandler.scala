/*
 * Copyright 2012 Comcast Cable Communications Management, LLC
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

import com.comcast.money.api.SpanInfo
import com.typesafe.config.Config
import org.slf4j.{ Logger, LoggerFactory }

object LoggingSpanHandler {

  type LogFunction = String => Unit

  val HEADER_FORMAT: String = "Span: [ span-id=%s ][ trace-id=%s ][ parent-id=%s ][ span-name=%s ][ " +
    "app-name=%s ][ start-time=%s ][ span-duration=%s ][ span-success=%s ] [ host=%s ]"
  val NOTE_BEGIN = "[ "
  val NOTE_END = " ]"
  val EQ = "="
  val NULL: String = "NULL"
}

class LoggingSpanHandler(val logger: Logger, makeFormatter: Config => SpanLogFormatter) extends ConfigurableHandler {

  def this() = this(LoggerFactory.getLogger(classOf[LoggingSpanHandler]), SpanLogFormatter.apply)

  import LoggingSpanHandler._

  protected var logFunction: LogFunction = logger.info
  protected var formatter: SpanLogFormatter = _

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

    val formattingConfig = config.getConfig("formatting")
    formatter = makeFormatter(formattingConfig)
  }

  def handle(spanInfo: SpanInfo): Unit = {
    logFunction(formatter.buildMessage(spanInfo))
  }
}

