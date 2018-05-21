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

package com.comcast.money.akka.http.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.stream.scaladsl.Flow
import com.comcast.money.akka.http.DefaultRequestSpanKeyCreators.DefaultSent
import com.comcast.money.akka.http.SentRequestSpanKeyCreator
import com.comcast.money.akka.{ MoneyExtension, SpanContextWithStack }

import scala.util.{ Failure, Success, Try }

object TracedHttpClientFlow {
  def apply[T](host: String, port: Int)(implicit actorSystem: ActorSystem, moneyExtension: MoneyExtension, hSKC: SentRequestSpanKeyCreator = DefaultSent): Flow[((HttpRequest, T), SpanContextWithStack), ((Try[HttpResponse], T), SpanContextWithStack), _] =
    Flow[((HttpRequest, T), SpanContextWithStack)]
      .map {
        case ((request, t), spanContext) =>
          moneyExtension.tracer(spanContext).startSpan(hSKC.httpRequestToKey(request))
          (request, (t, spanContext))
      }
      .via {
        Http()
          .cachedHostConnectionPool[(T, SpanContextWithStack)](host, port)
          .map {
            case (successHttpResponse: Success[HttpResponse], (t, spanContext)) =>
              moneyExtension.tracer(spanContext).stopSpan()
              ((successHttpResponse, t), spanContext)
            case (failed: Failure[HttpResponse], (t, spanContext)) =>
              moneyExtension.tracer(spanContext).stopSpan(result = false)
              ((failed, t), spanContext)
          }
      }
}
