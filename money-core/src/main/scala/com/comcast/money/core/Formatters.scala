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

import scala.util.{ Failure, Success, Try }

object Formatters {

  private[core] val MoneyTraceHeader = "X-MoneyTrace"
  private[core] val B3TraceIdHeader = "X-B3-TraceId"
  private[core] val B3SpanIdHeader = "X-B3-SpanId"
  private[core] val B3ParentSpanIdHeader = "X-B3-ParentSpanId"
  private[core] val TraceParentHeader = "traceparent"

  private[core] val TraceParentPattern = """^(\d\d)-([0-9a-f]{32})-([0-9a-f]{16})-(\d\d)$""".r

  implicit class StringWithHexHeaderConversion(s: String) {
    def fromHexStringToLong: Long = java.lang.Long.parseUnsignedLong(s, 16)
    def toGuid: String = {
      val spad = if (s.length < 32) s.padTo(32, 0).mkString else s
      String.format(
        "%s-%s-%s-%s-%s",
        spad.substring(0, 8),
        spad.substring(8, 12),
        spad.substring(12, 16),
        spad.substring(16, 20),
        spad.substring(20, 32))
    }
    def fromGuid: String = s.replace("-", "").toLowerCase
  }

  private[core] val MoneyHeaderFormat = "trace-id=%s;parent-id=%s;span-id=%s"
  private[core] val TraceParentHeaderFormat = "00-%s-%016x-00"

  def fromHttpHeaders(getHeader: String => String, log: String => Unit = _ => {}): Option[SpanId] =
    fromMoneyHeader(getHeader, log)
      .orElse(fromB3HttpHeaders(getHeader, log))
      .orElse(fromTraceParentHeader(getHeader, log))

  def toHttpHeaders(spanId: SpanId, addHeader: (String, String) => Unit): Unit = {
    toMoneyHeader(spanId, addHeader)
    toB3Headers(spanId, addHeader)
    toTraceParentHeader(spanId, addHeader)
  }

  private[core] def fromMoneyHeader(getHeader: String => String, log: String => Unit = _ => {}): Option[SpanId] = {

    def spanIdfromMoneyHeader(httpHeader: String) = Try {
      val parts = httpHeader.split(';')
      val traceId = parts(0).split('=')(1)
      val parentId = parts(1).split('=')(1)
      val selfId = parts(2).split('=')(1)

      new SpanId(traceId, parentId.toLong, selfId.toLong)
    }

    def parseHeader(headerValue: String): Option[SpanId] =
      spanIdfromMoneyHeader(headerValue) match {
        case Success(spanId) => Some(spanId)
        case Failure(ex) =>
          log(s"Unable to parse money trace for request header $headerValue")
          None
      }

    Option(getHeader(MoneyTraceHeader)).flatMap(parseHeader)
  }

  private[core] def toMoneyHeader(spanId: SpanId, addHeader: (String, String) => Unit): Unit = {
    addHeader(MoneyTraceHeader, MoneyHeaderFormat.format(spanId.traceId, spanId.parentId, spanId.selfId))
  }

  private[core] def fromB3HttpHeaders(getHeader: String => String, log: String => Unit = _ => {}): Option[SpanId] = {

    def spanIdFromB3Headers(traceId: String, maybeParentSpanId: Option[String], maybeSpanId: Option[String]): Try[SpanId] = Try {
      (maybeParentSpanId, maybeSpanId) match {
        case (Some(ps), Some(s)) => new SpanId(traceId.toGuid, ps.fromHexStringToLong, s.fromHexStringToLong)
        case (Some(ps), _) => new SpanId(traceId.toGuid, ps.fromHexStringToLong)
        case (_, Some(s)) => new SpanId(traceId.toGuid, s.fromHexStringToLong, s.fromHexStringToLong) // root span
        case _ => new SpanId(traceId.toGuid)
      }
    }

    def parseHeaders(traceIdVal: String): Option[SpanId] = {
      val maybeB3ParentSpanId = Option(getHeader(B3ParentSpanIdHeader))
      val maybeB3SpanId = Option(getHeader(B3SpanIdHeader))
      spanIdFromB3Headers(traceIdVal, maybeB3ParentSpanId, maybeB3SpanId) match {
        case Success(spanId) => Some(spanId)
        case Failure(ex) =>
          log(
            s"Unable to parse X-B3 trace for request headers: " +
              s"$B3TraceIdHeader:'$traceIdVal', " +
              s"$B3ParentSpanIdHeader:'$maybeB3ParentSpanId', " +
              s"$B3SpanIdHeader:'$maybeB3SpanId' " +
              s"${ex.getMessage}")
          None
      }
    }

    Option(getHeader(B3TraceIdHeader)).flatMap(parseHeaders)
  }

  private[core] def toB3Headers(spanId: SpanId, addHeader: (String, String) => Unit): Unit = {
    val formatGuid = {
      // X-B3 style traceId's can be 8 octets long
      val traceIdHex = spanId.traceId.fromGuid
      if (traceIdHex.endsWith("0" * 16)) traceIdHex.substring(0, 16)
      else traceIdHex
    }
    addHeader(B3TraceIdHeader, formatGuid)
    // No X-b3 parent if this is a root span
    if (spanId.parentId != spanId.selfId) addHeader(B3ParentSpanIdHeader, f"${spanId.parentId}%016x")
    addHeader(B3SpanIdHeader, f"${spanId.selfId}%016x")
  }

  private[core] def fromTraceParentHeader(getHeader: String => String, log: String => Unit = _ => {}): Option[SpanId] = {

    def spanIdFromHeader(traceId: String, parentSpanId: String): Try[SpanId] = Try {
      val parentSpanIdAsLong = parentSpanId.fromHexStringToLong
      new SpanId(traceId.toGuid, parentSpanIdAsLong, parentSpanIdAsLong)
    }

    def parseHeader(traceParentHeader: String): Option[SpanId] = {
      traceParentHeader match {
        case TraceParentPattern(_, traceId, parentSpanId, _) => spanIdFromHeader(traceId, parentSpanId) match {
          case Success(value) => Some(value)
          case Failure(ex) =>
            log(
              s"Unable to parse Trace-Context trace for request headers: " +
                s"$TraceParentHeader:'$traceParentHeader' " +
                s"${ex.getMessage}")
            None
        }
        case _ => None
      }
    }

    Option(getHeader(TraceParentHeader)).flatMap(parseHeader)
  }

  private[core] def toTraceParentHeader(spanId: SpanId, addHeader: (String, String) => Unit): Unit =
    addHeader(TraceParentHeader, TraceParentHeaderFormat.format(spanId.traceId.fromGuid, spanId.selfId))

  def setResponseHeaders(getHeader: String => String, addHeader: (String, String) => Unit) {
    def setResponseHeader(headerName: String): Unit = Option(getHeader(headerName)).foreach(v => addHeader(headerName, v))
    Seq(MoneyTraceHeader, B3TraceIdHeader, B3ParentSpanIdHeader, B3SpanIdHeader, TraceParentHeader).foreach(setResponseHeader)
  }
}
