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

import com.comcast.money.api.{SpanHandler, SpanInfo}
import com.typesafe.config.Config
import org.slf4j.{Logger, LoggerFactory}

object LoggingSpanHandler {

  val HEADER_FORMAT: String = "Span: [ span-id=%s ][ trace-id=%s ][ parent-id=%s ][ span-name=%s ][ " +
    "app-name=%s ][ start-time=%s ][ span-duration=%s ][ span-success=%s ] [ host=%s ]"
  val NOTE_BEGIN = "[ "
  val NOTE_END = " ]"
  val EQ = "="
  val NULL: String = "NULL"

  def apply(config: Config): LoggingSpanHandler =
    apply(LoggerFactory.getLogger(classOf[LoggingSpanHandler]), config)

  def apply(logger: Logger, config: Config): LoggingSpanHandler = {
    val logFunction = LogFunction(logger, config)
    val formatter = SpanLogFormatter(config)
    new LoggingSpanHandler(logFunction, formatter)
  }
}

class LoggingSpanHandler(logFunction: String => Unit, formatter: SpanLogFormatter) extends SpanHandler {

  def handle(spanInfo: SpanInfo): Unit = {
    logFunction(formatter.buildMessage(spanInfo))
  }
}

