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
import com.comcast.money.core.context.StructuredMdcContextStorageFilter.{ DefaultParentIdKey, DefaultSpanIdKey, DefaultSpanNameKey, DefaultTraceIdKey }
import com.comcast.money.core.internal.{ SpanContext, SpanLocal }
import com.typesafe.config.Config
import org.slf4j.MDC
import org.slf4j.spi.MDCAdapter

object StructuredMdcContextStorageFilter {
  private val DefaultTraceIdKey = "trace-id"
  private val DefaultSpanIdKey = "span-id"
  private val DefaultParentIdKey = "parent-id"
  private val DefaultSpanNameKey = "span-name"
  private val FormatIdsAsHexKey = "format-ids-as-hex"

  def apply(conf: Config): StructuredMdcContextStorageFilter = apply(conf, SpanLocal, MDC.getMDCAdapter)

  def apply(conf: Config, spanContext: SpanContext, mdc: MDCAdapter): StructuredMdcContextStorageFilter = {

    val traceIdKey = readConfString(conf, DefaultTraceIdKey, DefaultTraceIdKey)
    val spanIdKey = readConfString(conf, DefaultSpanIdKey, DefaultSpanIdKey)
    val parentIdKey = readConfString(conf, DefaultParentIdKey, DefaultParentIdKey)
    val spanNameKey = readConfString(conf, DefaultSpanNameKey, DefaultSpanNameKey)
    val formatIdsAsHex = conf.hasPath(FormatIdsAsHexKey) &&
      conf.getBoolean(FormatIdsAsHexKey)

    new StructuredMdcContextStorageFilter(spanContext, mdc, traceIdKey, spanIdKey, parentIdKey, spanNameKey, formatIdsAsHex)
  }

  private def readConfString(conf: Config, key: String, defaultValue: String) =
    if (conf.hasPath(key)) {
      conf.getString(key)
    } else defaultValue
}

class StructuredMdcContextStorageFilter(
  val spanContext: SpanContext,
  mdc: MDCAdapter,
  traceIdKey: String = DefaultTraceIdKey,
  spanIdKey: String = DefaultSpanIdKey,
  parentSpanIdKey: String = DefaultParentIdKey,
  spanNameKey: String = DefaultSpanNameKey,
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
