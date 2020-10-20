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

object B3MultiHeaderFormatter extends Formatter {
  private[core] val B3TraceIdHeader = "X-B3-TraceId"
  private[core] val B3SpanIdHeader = "X-B3-SpanId"
  private[core] val B3ParentSpanIdHeader = "X-B3-ParentSpanId"
  private[core] val B3SampledHeader = "X-B3-Sampled"
  private[core] val B3FlagsHeader = "X-B3-Flags"

  override def toHttpHeaders(spanId: SpanId, addHeader: (String, String) => Unit): Unit = {
    val formatGuid = {
      // X-B3 style traceId's can be 8 octets long
      val traceIdHex = spanId.traceIdAsHex
      if (traceIdHex.endsWith("0" * 16)) traceIdHex.substring(0, 16)
      else traceIdHex
    }
    addHeader(B3TraceIdHeader, formatGuid)
    // No X-b3 parent if this is a root span
    if (spanId.parentId != spanId.selfId) addHeader(B3ParentSpanIdHeader, spanId.parentIdAsHex)
    addHeader(B3SpanIdHeader, spanId.selfIdAsHex)
    addHeader(B3SampledHeader, if (spanId.isSampled) "1" else "0")
  }

  override def fromHttpHeaders(getHeader: String => String, log: String => Unit): Option[SpanId] = {

    def spanIdFromB3Headers(
      traceId: String,
      maybeParentSpanId: Option[String],
      maybeSpanId: Option[String],
      maybeSampled: Option[String],
      maybeFlags: Option[String]): Try[SpanId] = Try {

      val traceFlags = (maybeSampled, maybeFlags) match {
        case (_, Some("1")) => TraceFlags.getSampled
        case (Some(value), _) => value match {
          case "1" => TraceFlags.getSampled
          case "true" => TraceFlags.getSampled
          case _ => TraceFlags.getDefault
        }
        case _ => TraceFlags.getSampled
      }

      (maybeParentSpanId, maybeSpanId) match {
        case (Some(ps), Some(s)) =>
          SpanId.createRemote(SpanId.parseTraceIdFromHex(traceId), SpanId.parseIdFromHex(ps), SpanId.parseIdFromHex(s), traceFlags, TraceState.getDefault)
        case (Some(ps), _) =>
          SpanId.createRemote(SpanId.parseTraceIdFromHex(traceId), SpanId.parseIdFromHex(ps), SpanId.randomNonZeroLong(), traceFlags, TraceState.getDefault)
        case (_, Some(s)) =>
          val selfId = SpanId.parseIdFromHex(s)
          SpanId.createRemote(SpanId.parseTraceIdFromHex(traceId), selfId, selfId, traceFlags, TraceState.getDefault) // root span
        case _ =>
          val selfId = SpanId.randomNonZeroLong()
          SpanId.createRemote(SpanId.parseTraceIdFromHex(traceId), selfId, selfId, traceFlags, TraceState.getDefault)
      }
    }

    def parseHeaders(traceIdVal: String): Option[SpanId] = {
      val maybeB3ParentSpanId = Option(getHeader(B3ParentSpanIdHeader))
      val maybeB3SpanId = Option(getHeader(B3SpanIdHeader))
      val maybeB3Sampled = Option(getHeader(B3SampledHeader))
      val maybeB3Flags = Option(getHeader(B3FlagsHeader))

      spanIdFromB3Headers(traceIdVal, maybeB3ParentSpanId, maybeB3SpanId, maybeB3Sampled, maybeB3Flags) match {
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

  override def fields: Seq[String] = Seq(B3TraceIdHeader, B3SpanIdHeader, B3ParentSpanIdHeader, B3SampledHeader)
}
