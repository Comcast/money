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

package com.comcast.money.core.akka.http

import akka.http.impl.util.Rendering
import com.comcast.money.api.{ Span, SpanId }
import com.comcast.money.core.{ DisabledSpan, Formatters, NilSpanId, Tracer }
import com.comcast.money.core.akka.StackedSpanContext
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{ Directive, Directive1, RequestContext }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.BasicDirectives.{ extract => _, provide => _, _ }

import scala.util.{ Failure, Success }

case class `X-MoneyTraceHeader`(spanId: SpanId) {

  def name(): String = `X-MoneyTraceHeader`.headerName

  def value(): String = spanId match {
    case NilSpanId => ""
    case spanId => Formatters.toHttpHeader(spanId)
  }
}

object `X-MoneyTraceHeader` {
  val headerName = "X-MoneyTrace"

  def apply(implicit spanContext: StackedSpanContext): `X-MoneyTraceHeader` = apply(spanContext.current)
  def apply(tracer: Tracer): `X-MoneyTraceHeader` = apply(tracer.spanContext.current)
  def apply(span: Option[Span]): `X-MoneyTraceHeader` = apply(span.getOrElse(DisabledSpan))

  def apply(span: Span): `X-MoneyTraceHeader` = `X-MoneyTraceHeader`(span match {
    case DisabledSpan => NilSpanId
    case span => span.info.id
  })

  implicit def toHttpHeader(moneyTraceHeader: `X-MoneyTraceHeader`): HttpHeader = {
    RawHeader(headerName, moneyTraceHeader.value)
  }

  def withSpanId: Directive1[Option[SpanId]] = {
    val lowerCaseName = headerName.toLowerCase
    extract(_.request.headers.collectFirst {
      case HttpHeader(`lowerCaseName`, value) => SpanId.fromString(value)
    })
  }

}
