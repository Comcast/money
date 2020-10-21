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

package com.comcast.money.core.formatters

import com.comcast.money.api.SpanId
import io.opentelemetry.trace.{ TraceFlags, TraceState }

import scala.util.{ Failure, Success, Try }

object MoneyTraceFormatter {
  private[core] val MoneyTraceHeader = "X-MoneyTrace"
  private[core] val MoneyHeaderFormat = "trace-id=%s;parent-id=%s;span-id=%s"
}

class MoneyTraceFormatter extends Formatter {
  import com.comcast.money.core.formatters.MoneyTraceFormatter.{ MoneyHeaderFormat, MoneyTraceHeader }

  override def toHttpHeaders(spanId: SpanId, addHeader: (String, String) => Unit): Unit =
    addHeader(MoneyTraceHeader, MoneyHeaderFormat.format(spanId.traceId, spanId.parentId, spanId.selfId))

  override def fromHttpHeaders(getHeader: String => String, log: String => Unit): Option[SpanId] = {
    def spanIdFromMoneyHeader(httpHeader: String): Try[SpanId] = Try {
      val parts = httpHeader.split(';')
      val traceId = parts(0).split('=')(1)
      val parentId = parts(1).split('=')(1)
      val selfId = parts(2).split('=')(1)

      SpanId.createRemote(traceId, parentId.toLong, selfId.toLong, TraceFlags.getSampled, TraceState.getDefault)
    }

    def parseHeader(headerValue: String): Option[SpanId] =
      spanIdFromMoneyHeader(headerValue) match {
        case Success(spanId) => Some(spanId)
        case Failure(_) =>
          log(s"Unable to parse money trace for request header $headerValue")
          None
      }

    Option(getHeader(MoneyTraceHeader)).flatMap(parseHeader)
  }

  override def fields: Seq[String] = Seq(MoneyTraceHeader)
}
