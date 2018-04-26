/*
 * Copyright 2012-2015 Comcast Cable Communications Management, LLC
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

import scala.util.Try

object Formatters {

  private[core] val HttpHeaderFormat = "trace-id=%s;parent-id=%s;span-id=%s"

  def fromHttpHeader(httpHeader: String) = Try {
    val parts = httpHeader.split(';')
    val traceId = parts(0).split('=')(1)
    val parentId = parts(1).split('=')(1)
    val selfId = parts(2).split('=')(1)

    new SpanId(traceId, parentId.toLong, selfId.toLong)
  }

  def toHttpHeader(spanId: SpanId): String =
    HttpHeaderFormat.format(spanId.traceId, spanId.parentId, spanId.selfId)

  def fromB3HttpHeaders(traceId: String, maybeParentSpanId: Option[String], maybeSpanId: Option[String]) = Try {
    (maybeParentSpanId, maybeSpanId) match {
      case (Some(ps), Some(s)) => new SpanId(traceId, ps.toLong, s.toLong)
      case (Some(ps), _) => new SpanId(traceId, ps.toLong)
      case _ => new SpanId(traceId)
    }
  }
}
