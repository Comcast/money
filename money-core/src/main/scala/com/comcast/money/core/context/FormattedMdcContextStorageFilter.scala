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

object FormattedMdcContextStorageFilter {
  private val DefaultConfig = ConfigFactory.parseString(
    """
      |format-ids-as-hex = false
      |key = moneyTrace
      |format = "[ span-id=%2$s ][ trace-id=%1$s ][ parent-id=%3$s ][ span-name=%4$s ]"
      |""".stripMargin)

  private val MdcKeyKey = "key"
  private val FormatKey = "format"
  private val FormatIdsAsHexKey = "format-ids-as-hex"

  def apply(conf: Config): FormattedMdcContextStorageFilter = apply(conf, SpanLocal, MDC.getMDCAdapter)

  def apply(conf: Config, spanContext: SpanContext, mdc: MDCAdapter): FormattedMdcContextStorageFilter = {

    val effectiveConfig = conf.withFallback(DefaultConfig)
    val mdcKey = effectiveConfig.getString(MdcKeyKey)
    val format = effectiveConfig.getString(FormatKey)
    val formatIdsAsHex = effectiveConfig.getBoolean(FormatIdsAsHexKey)

    new FormattedMdcContextStorageFilter(spanContext, mdc, mdcKey, format, formatIdsAsHex)
  }
}

/**
 * Context storage filter that updates MDC properties when the span changes for the current thread.
 */
class FormattedMdcContextStorageFilter(
  override val spanContext: SpanContext,
  mdc: MDCAdapter,
  mdcKey: String,
  format: String,
  formatIdsAsHex: Boolean = false) extends AbstractMdcContextStorageFilter {

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
