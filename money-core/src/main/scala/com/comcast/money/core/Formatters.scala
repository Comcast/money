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

package com.comcast.money.core

import com.comcast.money.api.SpanId
import com.comcast.money.core.formatters.{ B3MultiHeaderFormatter, Formatter, FormatterChain, MoneyTraceFormatter, TraceContextFormatter }

/**
 * @deprecated Use `Money.Environment.formatter` to get the configured `Formatter`
 */
@deprecated("Use Money.Environment.formatter", "money-core 0.10.0")
@Deprecated
object Formatters extends Formatter {
  private[core] val formatter = Money.Environment.formatter

  def fromHttpHeaders(getHeader: String => String, log: String => Unit = _ => {}): Option[SpanId] =
    formatter.fromHttpHeaders(getHeader, log)

  def toHttpHeaders(spanId: SpanId, addHeader: (String, String) => Unit): Unit =
    formatter.toHttpHeaders(spanId, addHeader)

  override def fields: Seq[String] = formatter.fields

  override def setResponseHeaders(getHeader: String => String, addHeader: (String, String) => Unit): Unit =
    formatter.setResponseHeaders(getHeader, addHeader)
}
