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

import com.comcast.money.api.{ SpanHandler, SpanInfo }
import com.typesafe.config.Config
import org.slf4j.spi.MDCAdapter
import org.slf4j.{ LoggerFactory, MDC }

object StructuredLogSpanHandler {
  def apply(config: Config): StructuredLogSpanHandler = {
    val logger = LoggerFactory.getLogger(classOf[StructuredLogSpanHandler])
    val logFunction = LogFunction(logger, config)
    val formatIdsAsHex = config.hasPath("formatting.format-ids-as-hex") &&
      config.getBoolean("formatting.format-ids-as-hex")
    new StructuredLogSpanHandler(logFunction, MDC.getMDCAdapter, formatIdsAsHex)
  }
}

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
  val logFunction: String => Unit,
  val mdc: MDCAdapter,
  val formatIdsAsHex: Boolean) extends SpanHandler {

  def handle(spanInfo: SpanInfo): Unit = {
    import scala.collection.JavaConverters._
    val baseFields = Seq(
      // The field names below are the same as cedi-dtrace. This makes it easier to query a transaction in elastic search.
      ("trace-id", if (formatIdsAsHex) spanInfo.id.traceIdAsHex() else spanInfo.id.traceId()),
      ("parent-id", if (formatIdsAsHex) spanInfo.id.parentIdAsHex() else spanInfo.id.parentId()),
      ("span-id", if (formatIdsAsHex) spanInfo.id.selfIdAsHex() else spanInfo.id.selfId()),
      ("span-name", spanInfo.name()),
      ("app", spanInfo.appName()),
      ("host", spanInfo.host()),
      ("start-time", java.time.Instant.ofEpochMilli(spanInfo.startTimeMillis())),
      ("end-time", java.time.Instant.ofEpochMilli(spanInfo.endTimeMillis())),
      ("span-duration", spanInfo.durationMicros()),
      ("span-success", spanInfo.success()))
    val noteFields: Seq[(String, Any)] = spanInfo.notes.values.asScala.map(n => (n.name(), n.value())).toSeq
    val allFields = baseFields ++ noteFields

    allFields.foreach(p => mdc.put(p._1, p._2.toString))

    logFunction(allFields.map { case (k, v) => s"$k:$v" }.mkString("[", "][", "]"))
  }
}
