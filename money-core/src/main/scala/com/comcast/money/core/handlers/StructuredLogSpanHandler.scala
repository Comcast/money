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

import com.comcast.money.api.{ Note, SpanInfo }
import com.typesafe.config.Config
import org.slf4j.{ Logger, LoggerFactory, MDC }

/**
 * Logs using sfl4j MDC (mapped disagnostic context).
 * Logging  can be configured using a log configuration file such as logback.xml.
 * The MDC feature allows fields to be used by the appenders.
 * <p><p>
 * For example:
 * <p>
 * {@code <pattern>... trace id: %X{trace-id} etc </pattern>}
 * <p><p>
 * The following encoder configuration uses the MDC fields to create ELK compliant json logs
 * <p>
 * {@code <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>}
 *
 */
class StructuredLogSpanHandler(
  val logger: Logger = LoggerFactory.getLogger(classOf[StructuredLogSpanHandler]),
  val mdcFunc: (String, String) => Unit = (x: String, y: String) => MDC.put(x, y))
  extends ConfigurableHandler {

  // Extra constructor because java spring programs have a problem with the default function in the constructor above.
  def this() = this(LoggerFactory.getLogger(classOf[StructuredLogSpanHandler]), (k: String, v: String) => MDC.put(k, v))

  import com.comcast.money.core.handlers.LoggingSpanHandler._

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
    import scala.collection.JavaConverters._
    val baseFields = Seq(
      // The field names below are the same as cedi-dtrace. This makes it easier to query a transaction in elastic search.
      ("trace-id", spanInfo.id.traceId()),
      ("parent-id", spanInfo.id.parentId()),
      ("span-id", spanInfo.id.selfId()),
      ("span-name", spanInfo.name()),
      ("app", spanInfo.appName()),
      ("host", spanInfo.host()),
      ("start-time", java.time.Instant.ofEpochMilli(spanInfo.startTimeMillis())),
      ("end-time", java.time.Instant.ofEpochMilli(spanInfo.endTimeMillis())),
      ("span-duration", spanInfo.durationMicros()),
      ("span-success", spanInfo.success()))
    val noteFields: Seq[(String, Any)] = spanInfo.notes.values.asScala.map(n => (n.name(), n.value())).toSeq
    val allFields = baseFields ++ noteFields

    allFields.foreach(p => mdcFunc(p._1, p._2.toString))

    logFunction(allFields.map { case (k, v) => s"$k:$v" }.mkString("[", "][", "]"))
  }
}
