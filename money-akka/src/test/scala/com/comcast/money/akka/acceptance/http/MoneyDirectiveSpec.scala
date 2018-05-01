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

package com.comcast.money.akka.acceptance.http

import akka.http.scaladsl.model.HttpHeader.ParsingResult.{Error, Ok}
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import com.comcast.money.akka.SpanHandlerMatchers.haveSomeSpanName
import com.comcast.money.akka.http._
import com.comcast.money.akka.{AkkaMoneyScope, SpanContextWithStack}
import com.comcast.money.api.Span
import com.comcast.money.core.Formatters

class MoneyDirectiveSpec extends AkkaMoneyScope {

  def testRoute(implicit requestSKC: HttpRequestSpanKeyCreator = DefaultHttpRequestSpanKeyCreator) =
    pathSingleSlash {
      get {
        MoneyTrace {
          (tracedRequest: TracedRequest) => TracedResponse(HttpResponse(entity = "response"), tracedRequest.spanContext)
        }
      }
    }

  "A Akka Http route with a MoneyDirective" should {
    "start a span for a request" in {
      Get("/") ~> testRoute ~> check(responseAs[String] shouldBe "response")

      maybeCollectingSpanHandler should haveSomeSpanName("GET /")
    }

    "continue a span for a request with a span" in {
      import scala.collection.immutable.Seq
      val tracedHttpRequest = "TracedHttpRequest"
      val span: Span = {
        implicit val spanContextWithStack = new SpanContextWithStack
        moneyExtension.tracer.startSpan(tracedHttpRequest)
        spanContextWithStack.current.get
      }

      val header =
        HttpHeader.parse(name = "X-MoneyTrace", value = Formatters.toHttpHeader(span.info.id)) match {
          case Ok(parsedHeader, _) => parsedHeader
          case Error(errorInfo) => throw ParseFailure(errorInfo.summary)
        }

      implicit val httpSKC: HttpRequestSpanKeyCreator = HttpRequestSpanKeyCreator((_: HttpRequest) => tracedHttpRequest)

      HttpRequest(headers = Seq(header)) ~> testRoute ~> check(responseAs[String] shouldBe "response")

      maybeCollectingSpanHandler should haveSomeSpanName(tracedHttpRequest)
    }
  }

  case class ParseFailure(msg: String) extends Throwable(msg)
}
