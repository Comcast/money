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

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, StreamTcpException}
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka.SpanHandlerMatchers.{haveSomeSpanName, haveSomeSpanNames, maybeCollectingSpanHandler}
import com.comcast.money.akka.http.client.{TracedHttpClient, TracedHttpFlow}
import com.comcast.money.akka.{AkkaMoneyScope, CollectingSpanHandler, SpanContextWithStack}
import org.scalatest.matchers.{MatchResult, Matcher}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class MoneyClientSpec extends AkkaMoneyScope {

  "A Akka Http Client traced with Money" when {
    "sending a traced request with a flow" should {
      "complete spans and add them to the Span Handler for a successful request" in {
        val eventualMaybeResponse =
          httpStream().run flatMap {
            case (Success(response), i) => Unmarshal(response).to[String].map((_, i))
            case (Failure(e), _) => Future.failed(e)
          }

        val responseEntityWithIdentifier = eventualMaybeResponse.get()
        responseEntityWithIdentifier shouldBe("response", 1)

        maybeCollectingSpanHandler should haveSomeSpanNames(Seq("Stream", "GET /"))
        maybeCollectingSpanHandler should not(haveFailedSpans)
      }

      "complete spans and add them to the Span Handler for a failed request" in {
        val eventualFailure = httpStream(unavailableHost).run flatMap {
          case (Success(response), _) => Unmarshal(response).to[String]
          case (Failure(e), _) => Future.failed(e)
        }

        Try(eventualFailure.get()) shouldBe a[Failure[StreamTcpException]]

        maybeCollectingSpanHandler should haveSomeSpanNames(Seq("Stream", "GET /"))
        maybeCollectingSpanHandler should haveFailedSpans
      }
    }

    "sending a request with a Future based API" should {
      "create spans and add them to the Span Handler for a successful request without a body" in {
        implicit val spanContext: SpanContextWithStack = new SpanContextWithStack

        val tracedClient = new TracedHttpClient()

        val responseEntity = tracedClient.get(localHostRootUriString).flatMap(Unmarshal(_).to[String]).get()

        responseEntity shouldBe "response"

        maybeCollectingSpanHandler should haveSomeSpanName("GET /")
        maybeCollectingSpanHandler should not(haveFailedSpans)
      }

      "create spans and add them to the Span Handler for a successful request with a body" in {
        implicit val spanContext: SpanContextWithStack = new SpanContextWithStack

        val tracedClient = new TracedHttpClient()

        val responseEntity = tracedClient.get(localHostRootUriString, Some("request")).flatMap(Unmarshal(_).to[String]).get()

        responseEntity shouldBe "response"

        maybeCollectingSpanHandler should haveSomeSpanName("GET /")
        maybeCollectingSpanHandler should not(haveFailedSpans)
      }

      "create spans and add them to the Span Handler for a failed request" in {
        implicit val spanContext: SpanContextWithStack = new SpanContextWithStack

        val tracedClient = new TracedHttpClient()

        val eventualFailure = tracedClient.get(uri = s"http://$unavailableHost:8080/")

        Try(eventualFailure.get()) shouldBe a[Failure[StreamTcpException]]

        Thread.sleep(10L) // onComplete is on a separate thread and can be run after the next statements are run

        maybeCollectingSpanHandler should haveSomeSpanName("GET /")
        maybeCollectingSpanHandler should haveFailedSpans
      }
    }
  }

  val localhost = "localhost"
  val unavailableHost = "unavailableHost"

  val localHostRootUriString = s"http://$localhost:8080/"

  val route: Route =
    get {
      pathSingleSlash {
        complete("response")
      }
    }

  def haveFailedSpans: Matcher[Option[CollectingSpanHandler]] =
    Matcher {
      maybeSpanHandler =>
        val maybeSpanInfoStack = maybeSpanHandler.map(_.spanInfoStack)

        val hasSpanFailure =
          maybeSpanInfoStack
            .exists {
              _.foldLeft(true) {
                case (status, _) if status == false => true
                case (_, spanInfo) if spanInfo.success == false => true
                case (_, _) => false
              }
            }

        MatchResult(
          matches = hasSpanFailure,
          rawFailureMessage = s"No Spans failed in $maybeSpanInfoStack",
          rawNegatedFailureMessage = s"Spans failed in $maybeSpanInfoStack"
        )
    }

  override def beforeAll(): Unit = Http().bindAndHandle(route, localhost, 8080)

  def httpStream(host: String = localhost): RunnableGraph[Future[(Try[HttpResponse], Int)]] =
    RunnableGraph fromGraph {
      GraphDSL.create(Sink.head[(Try[HttpResponse], Int)]) { implicit builder =>
        sink =>
          import com.comcast.money.akka.stream.StreamTracingDSL._

          Source(List((HttpRequest(), 1))) ~|> TracedHttpFlow[Int](host, 8080) ~| sink.in
          ClosedShape
      }
    }
}
