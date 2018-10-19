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

import java.util.Map

import com.comcast.money.api.SpanId
import com.comcast.money.core.Money
import org.slf4j.MDC

object MDCSupport {

  val LogFormat = "[ span-id=%s ][ trace-id=%s ][ parent-id=%s ]"

  def format(spanId: SpanId) = LogFormat.format(spanId.selfId, spanId.traceId, spanId.parentId)
}

/**
 * Adds the ability to store a span in MDC for a magical logging experience
 * @param enabled True if mdc is enabled, false if it is disabled
 */
class MDCSupport(enabled: Boolean = Money.Environment.enabled) {

  private val MoneyTraceKey = "moneyTrace"
  private val SpanNameKey = "spanName"

  def setSpanMDC(spanId: Option[SpanId]): Unit = if (enabled) {
    spanId match {
      case Some(id) => MDC.put(MoneyTraceKey, MDCSupport.format(id))
      case None => MDC.remove(MoneyTraceKey)
    }
  }

  def propogateMDC(submittingThreadsContext: Option[Map[_, _]]): Unit = if (enabled) {
    submittingThreadsContext match {
      case Some(context: Map[String, String]) => MDC.setContextMap(context)
      case None => MDC.clear()
    }
  }

  def setSpanNameMDC(spanName: Option[String]) =
    if (enabled) {
      spanName match {
        case Some(name) => MDC.put(SpanNameKey, name)
        case None => MDC.remove(SpanNameKey)
      }
    }

  def getSpanNameMDC: Option[String] = Option(MDC.get(SpanNameKey))
}
