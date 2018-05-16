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

import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.model.HttpHeader.ParsingResult.{ Error, Ok }
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Source
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka.SpanHandlerMatchers.{ haveSomeSpanName, maybeCollectingSpanHandler }
import com.comcast.money.akka.http._
import com.comcast.money.akka.{ AkkaMoneyScope, CollectingSpanHandler, TestStreams }
import com.comcast.money.api.SpanId
import com.comcast.money.core.Formatters
import org.scalatest.matchers.{ MatchResult, Matcher }

import scala.concurrent.duration.{ DurationDouble, FiniteDuration }
import scala.concurrent.{ ExecutionContext, Future }

class MoneyTraceSpec extends AkkaMoneyScope {

  "A Akka Http route with a MoneyDirective" should {
    "start a span for a request" in {
      Get("/") ~> simpleRoute() ~> check(responseAs[String] shouldBe "response")

      maybeCollectingSpanHandler should haveSomeSpanName(getRoot)
    }

    "continue a span for a request with a span" in {
      import scala.collection.immutable.Seq
      val parentSpanId: SpanId = new SpanId()

      val parentSpanIdHeader =
        HttpHeader.parse(name = "X-MoneyTrace", value = Formatters.toHttpHeader(parentSpanId)) match {
          case Ok(parsedHeader, _) => parsedHeader
          case Error(errorInfo) => throw ParseFailure(errorInfo.summary)
        }

      HttpRequest(headers = Seq(parentSpanIdHeader)) ~> simpleRoute() ~> check(responseAs[String] shouldBe "response")

      maybeCollectingSpanHandler should haveParentSpanId(parentSpanId)
    }

    "have the capacity to be named by a user" in {
      implicit val httpSKC: HttpRequestSpanKeyCreator = HttpRequestSpanKeyCreator((_: HttpRequest) => tracedHttpRequest)

      Get("/") ~> simpleRoute() ~> check(responseAs[String] shouldBe "response")

      maybeCollectingSpanHandler should haveSomeSpanName(tracedHttpRequest)
    }

    "trace a chunked response till it completes fully" in {
      Get("/chunked") ~> simpleRoute() ~> check {
        val pattern = "chunk([0-9]+)".r
        val entityString = pattern.findAllIn(Unmarshal(responseEntity).to[String].get()).toSeq

        entityString.take(3) shouldBe Seq("chunk1", "chunk2", "chunk3")
      }

      maybeCollectingSpanHandler should haveARequestDurationLongerThan(120 millis)
    }

    "trace a asynchronous request" in {
      Get("/async") ~> simpleRoute() ~> check(responseAs[String] shouldBe "asyncResponse")

      maybeCollectingSpanHandler should haveSomeSpanName("GET /async")
    }
  }

  def haveParentSpanId(parentId: SpanId): Matcher[Option[CollectingSpanHandler]] =
    Matcher {
      maybeCollectingSpanHandler =>
        val spanInfoStack = maybeCollectingSpanHandler.map(_.spanInfoStack.sortBy(_.startTimeMicros)).get

        val maybeParentId =
          spanInfoStack
            .flatMap {
              spanInfo =>
                if (spanInfo.id.parentId == parentId.selfId) Some(true)
                else None
            }
            .headOption

        MatchResult(
          matches = maybeParentId.getOrElse(false),
          rawFailureMessage = s"Parent Span Id was $maybeParentId not Some($parentId)",
          rawNegatedFailureMessage = s"Parent Span Id was $maybeParentId equal to Some($parentId)"
        )
    }

  def haveARequestDurationLongerThan(expectedTimeTaken: FiniteDuration): Matcher[Option[CollectingSpanHandler]] =
    Matcher {
      maybeCollectingSpanHandler =>
        val requestSpanName = "GET /chunked"
        val maybeSpanInfo =
          maybeCollectingSpanHandler
            .map(_.spanInfoStack)
            .flatMap(_.find(_.name == requestSpanName))

        val maybeMillis = maybeSpanInfo.map(_.durationMicros / 1000)
        MatchResult(
          matches =
          maybeSpanInfo match {
            case Some(spanInfo) => spanInfo.durationMicros >= expectedTimeTaken.toMicros
            case None => false
          },
          rawFailureMessage = s"Duration of Span $requestSpanName was $maybeMillis not Some($expectedTimeTaken)",
          rawNegatedFailureMessage = s"Duration of Span $requestSpanName was $maybeMillis equal to Some($expectedTimeTaken)"
        )
    }

  val testStreams = new TestStreams

  def simpleRoute(source: Source[ChunkStreamPart, _] = testStreams.asyncManyElements)(implicit
    requestSKC: HttpRequestSpanKeyCreator = DefaultHttpRequestSpanKeyCreator,
    executionContext: ExecutionContext) =
    get {
      pathSingleSlash {
        MoneyTrace {
          (_: TracedRequest) => TracedResponse(HttpResponse(entity = "response"))
        }
      } ~
        path("chunked") {
          MoneyTrace fromChunkedSource {
            (_: TracedRequest) => source
          }
        } ~
        path("async") {
          MoneyTrace {
            (_: TracedRequest) => Future(TracedResponse(HttpResponse(entity = "asyncResponse")))
          }
        }
    }

  val getRoot = "GET /"
  val tracedHttpRequest = "TracedHttpRequest"

  case class ParseFailure(msg: String) extends Throwable(msg)

}
