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

package com.comcast.money.core.context

import com.comcast.money.api.SpanInfo
import com.comcast.money.core.internal.{ SpanContext, SpanLocal }
import com.typesafe.config.{ Config, ConfigFactory }
import org.slf4j.MDC
import org.slf4j.spi.MDCAdapter

object StructuredMdcContextStorageFilter {
  private val DefaultConfig = ConfigFactory.parseString(
    """
      | format-ids-as-hex = false
      | trace-id = "trace-id"
      | span-id = "span-id"
      | parent-id = "parent-id"
      | span-name = "span-name"
      |""".stripMargin)

  private val TraceIdKey = "trace-id"
  private val SpanIdKey = "span-id"
  private val ParentIdKey = "parent-id"
  private val SpanNameKey = "span-name"
  private val FormatIdsAsHexKey = "format-ids-as-hex"

  def apply(conf: Config): StructuredMdcContextStorageFilter = apply(conf, SpanLocal, MDC.getMDCAdapter)

  def apply(conf: Config, spanContext: SpanContext, mdc: MDCAdapter): StructuredMdcContextStorageFilter = {

    val effectiveConfig = conf.withFallback(DefaultConfig)
    val traceIdKey = effectiveConfig.getString(TraceIdKey)
    val spanIdKey = effectiveConfig.getString(SpanIdKey)
    val parentIdKey = effectiveConfig.getString(ParentIdKey)
    val spanNameKey = effectiveConfig.getString(SpanNameKey)
    val formatIdsAsHex = effectiveConfig.getBoolean(FormatIdsAsHexKey)

    new StructuredMdcContextStorageFilter(spanContext, mdc, traceIdKey, spanIdKey, parentIdKey, spanNameKey, formatIdsAsHex)
  }
}

class StructuredMdcContextStorageFilter(
  val spanContext: SpanContext,
  mdc: MDCAdapter,
  traceIdKey: String,
  spanIdKey: String,
  parentSpanIdKey: String,
  spanNameKey: String,
  formatIdsAsHex: Boolean) extends MdcContextStorageFilter {

  override def updateMdc(currentSpanInfo: Option[SpanInfo]): Unit = currentSpanInfo match {
    case Some(info) if (formatIdsAsHex) =>
      val spanId = info.id
      mdc.put(traceIdKey, spanId.traceIdAsHex)
      mdc.put(spanIdKey, spanId.selfIdAsHex)
      mdc.put(parentSpanIdKey, spanId.parentIdAsHex)
      mdc.put(spanNameKey, info.name)
    case Some(info) =>
      val spanId = info.id
      mdc.put(traceIdKey, spanId.traceId)
      mdc.put(spanIdKey, spanId.selfId.toString)
      mdc.put(parentSpanIdKey, spanId.parentId.toString)
      mdc.put(spanNameKey, info.name)
    case None =>
      mdc.remove(traceIdKey)
      mdc.remove(spanIdKey)
      mdc.remove(parentSpanIdKey)
      mdc.remove(spanNameKey)
  }
}
