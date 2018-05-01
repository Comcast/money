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

package com.comcast.money.akka.http

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives.{complete, extractRequest}
import akka.http.scaladsl.server.Route
import com.comcast.money.akka.{MoneyExtension, SpanContextWithStack}
import com.comcast.money.api.SpanId
import com.comcast.money.core.Formatters.fromHttpHeader
import com.comcast.money.core.Tracer

import scala.util.{Failure, Success}

object MoneyTrace {
  def apply(f: TracedRequest ⇒ TracedResponse)
           (implicit moneyExtension: MoneyExtension,
            requestSKC: HttpRequestSpanKeyCreator): Route =
    extractRequest {
      request =>
        implicit val spanContext: SpanContextWithStack = new SpanContextWithStack
        maybeAddHeaderSpan(request, moneyExtension.tracer(spanContext))

        val tracedResponse = f(TracedRequest(request, spanContext))

        moneyExtension.tracer(tracedResponse.spanContext).stopSpan(tracedResponse.isSuccess)
        complete(tracedResponse.response)
    }

  private def maybeAddHeaderSpan(request: HttpRequest, tracer: Tracer)
                                (implicit spanContext: SpanContextWithStack,
                                 requestSKC: HttpRequestSpanKeyCreator) =
    request
      .headers
      .map(header => fromHttpHeader(header.value))
      .foldLeft[Option[SpanId]](None) {
        case (_, Success(spanId)) => Some(spanId)
        case (Some(spanId), _) => Some(spanId)
        case (_, _: Failure[SpanId]) => None
      }
      .map {
        spanId =>
          val freshSpanId = new SpanId(spanId.traceId, spanId.parentId)
          spanContext.push(tracer.spanFactory.newSpan(freshSpanId, requestSKC.httpRequestToKey(request)))
          spanContext
      }
      .getOrElse {
        tracer.startSpan(requestSKC.httpRequestToKey(request))
        spanContext
      }
}

case class TracedRequest(request: HttpRequest, spanContext: SpanContextWithStack)

case class TracedResponse(response: HttpResponse, spanContext: SpanContextWithStack, isSuccess: Boolean = true)
