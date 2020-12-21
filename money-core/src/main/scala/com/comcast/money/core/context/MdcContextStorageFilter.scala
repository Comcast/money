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
import com.comcast.money.core.internal.{ SpanContext, SpanLocal }
import com.typesafe.config.Config
import io.opentelemetry.context.{ Context, ContextStorage, Scope }
import org.slf4j.MDC
import org.slf4j.spi.MDCAdapter

object MdcContextStorageFilter {
  def apply(conf: Config): MdcContextStorageFilter = {
    val formatIdsAsHex = conf.hasPath("format-ids-as-hex") &&
      conf.getBoolean("format-ids-as-hex")
    new MdcContextStorageFilter(SpanLocal, MDC.getMDCAdapter, formatIdsAsHex)
  }
}

/**
 * Context storage filter that updates MDC properties when the span changes for the current thread.
 */
class MdcContextStorageFilter(spanContext: SpanContext, mdc: MDCAdapter, hex: Boolean) extends ContextStorageFilter {
  private val LogFormat = "[ span-id=%s ][ trace-id=%s ][ parent-id=%s ]"
  private val MoneyTraceKey = "moneyTrace"
  private val SpanNameKey = "spanName"

  override def attach(context: Context, storage: ContextStorage): Scope = {
    val scope = storage.attach(context)
    updateMdc(context)
    () => {
      scope.close()
      updateMdc(storage.current)
    }
  }

  def updateMdc(context: Context): Unit =
    spanContext.fromContext(context)
      .map(_.info) match {
        case Some(info) if hex =>
          val spanId = info.id
          mdc.put(MoneyTraceKey, LogFormat.format(spanId.selfIdAsHex, spanId.traceIdAsHex, spanId.parentIdAsHex))
          mdc.put(SpanNameKey, info.name)
        case Some(info) =>
          val spanId = info.id
          mdc.put(MoneyTraceKey, LogFormat.format(spanId.selfId, spanId.traceId, spanId.parentId))
          mdc.put(SpanNameKey, info.name)
        case None =>
          mdc.remove(MoneyTraceKey)
          mdc.remove(SpanNameKey)
      }
}
