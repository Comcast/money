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

object Formatters extends Formatter {
  //TODO: make configurable!
  private[core] val chain = FormatterChain(Seq(MoneyTraceFormatter, B3MultiHeaderFormatter, TraceContextFormatter))

  def fromHttpHeaders(getHeader: String => String, log: String => Unit = _ => {}): Option[SpanId] =
    chain.fromHttpHeaders(getHeader, log)

  def toHttpHeaders(spanId: SpanId, addHeader: (String, String) => Unit): Unit =
    chain.toHttpHeaders(spanId, addHeader)

  override def fields: Seq[String] = chain.fields

  override def setResponseHeaders(getHeader: String => String, addHeader: (String, String) => Unit): Unit =
    chain.setResponseHeaders(getHeader, addHeader)
}
