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
import com.comcast.money.core.context.FormattedMdcContextStorageFilter.{ DefaultFormat, DefaultMdcKey }
import com.comcast.money.core.internal.{ SpanContext, SpanLocal }
import com.typesafe.config.Config
import org.slf4j.MDC
import org.slf4j.spi.MDCAdapter

object FormattedMdcContextStorageFilter {
  private val DefaultMdcKey = "moneyTrace"
  private val DefaultFormat = "[ span-id=%2$s ][ trace-id=%1$s ][ parent-id=%3$s ][ span-name=%4$s ]"
  private val FormatIdsAsHexKey = "format-ids-as-hex"

  def apply(conf: Config): FormattedMdcContextStorageFilter = apply(conf, SpanLocal, MDC.getMDCAdapter)

  def apply(conf: Config, spanContext: SpanContext, mdc: MDCAdapter): FormattedMdcContextStorageFilter = {

    val mdcKey = readConfString(conf, "key", DefaultMdcKey)
    val format = readConfString(conf, "format", DefaultFormat)
    val formatIdsAsHex = conf.hasPath(FormatIdsAsHexKey) &&
      conf.getBoolean(FormatIdsAsHexKey)

    new FormattedMdcContextStorageFilter(spanContext, mdc, mdcKey, format, formatIdsAsHex)
  }

  private def readConfString(conf: Config, key: String, defaultValue: String) =
    if (conf.hasPath(key)) {
      conf.getString(key)
    } else defaultValue
}

/**
 * Context storage filter that updates MDC properties when the span changes for the current thread.
 */
class FormattedMdcContextStorageFilter(
  val spanContext: SpanContext,
  mdc: MDCAdapter,
  mdcKey: String = DefaultMdcKey,
  format: String = DefaultFormat,
  formatIdsAsHex: Boolean = false) extends MdcContextStorageFilter {

  override def updateMdc(currentSpanInfo: Option[SpanInfo]): Unit = currentSpanInfo match {
    case Some(info) =>
      val spanId = info.id
      val moneyTrace = if (formatIdsAsHex) {
        format.format(spanId.traceIdAsHex(), spanId.selfIdAsHex(), spanId.parentIdAsHex(), info.name)
      } else {
        format.format(spanId.traceId(), spanId.selfId(), spanId.parentId(), info.name)
      }
      mdc.put(mdcKey, moneyTrace)
    case None =>
      mdc.remove(mdcKey)
  }
}
