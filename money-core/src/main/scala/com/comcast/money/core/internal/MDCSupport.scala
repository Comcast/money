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

package com.comcast.money.core.internal

import java.util
import java.util.Map

import com.comcast.money.api.{ Span, SpanId }
import com.comcast.money.core.Money
import org.slf4j.MDC

object MDCSupport {

  val LogFormat = "[ span-id=%s ][ trace-id=%s ][ parent-id=%s ]"
  val LogHexFormat = "[ span-id=%016x ][ trace-id=%s ][ parent-id=%016x ]"

  def format(spanId: SpanId): String = format(spanId, formatIdsAsHex = false)

  def format(spanId: SpanId, formatIdsAsHex: Boolean): String = if (formatIdsAsHex) {
    LogHexFormat.format(
      spanId.selfId,
      spanId.traceId.replace("-", "").toLowerCase,
      spanId.parentId)
  } else {
    LogFormat.format(spanId.selfId, spanId.traceId, spanId.parentId)
  }
}

/**
 * Adds the ability to store a span in MDC for a magical logging experience
 * @param enabled True if mdc is enabled, false if it is disabled
 */
class MDCSupport(
  enabled: Boolean = Money.Environment.enabled,
  formatIdsAsHex: Boolean = Money.Environment.formatIdsAsHex) {

  private val MoneyTraceKey = "moneyTrace"
  private val SpanNameKey = "spanName"

  def setSpanMDC(span: Option[Span]): Unit = if (enabled) {
    span match {
      case Some(s) =>
        MDC.put(MoneyTraceKey, MDCSupport.format(s.info.id, formatIdsAsHex))
        MDC.put(SpanNameKey, s.info.name)
      case None =>
        MDC.remove(MoneyTraceKey)
        MDC.remove(SpanNameKey)
    }
  }

  def getCopyOfMDC: Option[util.Map[String, String]] = Option(MDC.getCopyOfContextMap)

  def propagateMDC(submittingThreadsContext: Option[util.Map[String, String]]): Unit = if (enabled) {
    submittingThreadsContext match {
      case Some(context: util.Map[String, String]) => MDC.setContextMap(context)
      case None => MDC.clear()
    }
  }
}
