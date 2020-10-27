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
import com.comcast.money.core.formatters.MoneyTraceFormatter.{KeyValueDelimiter, MoneyStateDelimiter, MoneyStateHeader, ParentIdKey, SampledKey, SpanIdKey, TraceIdKey}
import io.opentelemetry.trace.{TraceFlags, TraceState}

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

object MoneyTraceFormatter {
  private[core] val MoneyTraceHeader = "X-MoneyTrace"
  private[core] val MoneyStateHeader = "X-MoneyState"

  private[core] val MoneyHeaderFormat = "trace-id=%s;parent-id=%s;span-id=%s"
  private[core] val MoneyStateDelimiter = """[ \t]*;[ \t]*""".r
  private[core] val KeyValueDelimiter = """[ \t]*=[ \t]*""".r

  private[core] val TraceIdKey = "trace-id"
  private[core] val ParentIdKey = "parent-id"
  private[core] val SpanIdKey = "span-id"
  private[core] val SampledKey = "sampled"
}

final class MoneyTraceFormatter extends Formatter {
  import com.comcast.money.core.formatters.MoneyTraceFormatter.{ MoneyHeaderFormat, MoneyTraceHeader }

  override def toHttpHeaders(spanId: SpanId, addHeader: (String, String) => Unit): Unit = {
    addHeader(MoneyTraceHeader, MoneyHeaderFormat.format(spanId.traceId, spanId.parentId, spanId.selfId))

    val sampled = ("sampled", if (spanId.isSampled) "1" else "0")
    val stateEntries = sampled +: spanId.traceState.getEntries.asScala.toStream
      .map { entry => entry.getKey -> entry.getValue }

    addHeader(MoneyStateHeader, stateEntries.map { case (k, v) => s"$k=$v"}.mkString(";"))
  }

  override def fromHttpHeaders(getHeader: String => String, log: String => Unit): Option[SpanId] = {
    def parseHeaderMap(header: String): Map[String, String] = MoneyStateDelimiter.split(header)
        .map { KeyValueDelimiter.split }
        .filter { _.length == 2 }
        .map { case Array(key, value) => key -> value }
        .toMap

    def parseStateHeader(stateHeader: String): (Byte, TraceState) = {

      var flags: Byte = TraceFlags.getSampled
      val stateBuilder = TraceState.builder
      parseHeaderMap(stateHeader).foreach {
        case (SampledKey, "1") => flags = TraceFlags.getSampled
        case (SampledKey, _) => flags = TraceFlags.getDefault
        case (key, value) => stateBuilder.set(key, value)
      }

      (flags, stateBuilder.build)
    }

    def parseLong(value: String): Option[Long] = Try { value.toLong } toOption

    def spanIdFromMoneyHeader(httpHeader: String, stateHeader: Option[String]): Try[Option[SpanId]] = Try {

      val map = parseHeaderMap(httpHeader)
      for {
        traceId <- map.get(TraceIdKey)
        parentId <- map.get(ParentIdKey) flatMap { parseLong }
        selfId <- map.get(SpanIdKey) flatMap { parseLong }
        (flags, state) = stateHeader.map { parseStateHeader } getOrElse (TraceFlags.getSampled, TraceState.getDefault)
      } yield SpanId.createRemote(traceId, parentId, selfId, flags, state)
    }

    def parseHeader(headerValue: String): Option[SpanId] =
      spanIdFromMoneyHeader(headerValue, Option(getHeader(MoneyStateHeader))) match {
        case Success(Some(spanId)) => Some(spanId)
        case _ =>
          log(s"Unable to parse money trace for request header '$headerValue'")
          None
      }

    Option(getHeader(MoneyTraceHeader)).flatMap(parseHeader)
  }

  override def fields: Seq[String] = Seq(MoneyTraceHeader, MoneyStateHeader)
}
