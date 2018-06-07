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
  implicit class StringHexHelpers(s: String) {
    def fromHexStringToLong: Long = java.lang.Long.parseUnsignedLong(s, 16)
    def toGuid: String = {
      val spad = if (s.length < 32) s.padTo(32, 0).mkString else s
      String.format(
        "%s-%s-%s-%s-%s",
        spad.substring(0, 8),
        spad.substring(8, 12),
        spad.substring(12, 16),
        spad.substring(16, 20),
        spad.substring(20, 32)
      )
    }
    def fromGuid: String = s.replace("-", "")
  }

  private[core] val HttpHeaderFormat = "trace-id=%s;parent-id=%s;span-id=%s"

  def fromHttpHeader(httpHeader: String) = Try {
    val parts = httpHeader.split(';')
    val traceId = parts(0).split('=')(1)
    val parentId = parts(1).split('=')(1)
    val selfId = parts(2).split('=')(1)

    new SpanId(traceId, parentId.toLong, selfId.toLong)
  }

  def toHttpHeader(spanId: SpanId): String = HttpHeaderFormat.format(spanId.traceId, spanId.parentId, spanId.selfId)

  def fromB3HttpHeaders(traceId: String, maybeParentSpanId: Option[String], maybeSpanId: Option[String]) = Try {
    (maybeParentSpanId, maybeSpanId) match {
      case (Some(ps), Some(s)) => new SpanId(traceId.toGuid, ps.fromHexStringToLong, s.fromHexStringToLong)
      case (Some(ps), _) => new SpanId(traceId.toGuid, ps.fromHexStringToLong)
      case (_, Some(s)) => new SpanId(traceId.toGuid, s.fromHexStringToLong, s.fromHexStringToLong) // root span
      case _ => new SpanId(traceId.toGuid)
    }
  }

  def toB3Headers(spanId: SpanId)(ft: String => Unit, fps: String => Unit, fs: String => Unit): Unit = {
    def formatGuid = {
      // X-B3 style traceId's can be 8 octets long
      val traceIdHex = spanId.traceId.fromGuid
      if (traceIdHex.endsWith("0" * 16)) traceIdHex.substring(0, 16) else traceIdHex
    }
    ft(formatGuid)
    if (spanId.parentId != spanId.selfId) fps(spanId.parentId.toHexString) // No X-b3 parent if this is a root span
    fs(spanId.selfId.toHexString)
  }
}
