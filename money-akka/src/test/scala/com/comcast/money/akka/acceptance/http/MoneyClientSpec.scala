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
import akka.stream.ClosedShape
import akka.stream.scaladsl.{ GraphDSL, RunnableGraph, Sink, Source }
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka.SpanHandlerMatchers.{ haveSomeSpanName, haveSomeSpanNames, maybeCollectingSpanHandler }
import com.comcast.money.akka.http.SentRequestSpanKeyCreator
import com.comcast.money.akka.http.client.{ TracedHttpClient, TracedHttpClientFlow }
import com.comcast.money.akka.http.server.{ MoneyTrace, TracedResponse }
import com.comcast.money.akka.{ AkkaMoneyScope, CollectingSpanHandler, SpanContextWithStack }
import org.scalatest.matchers.{ MatchResult, Matcher }

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

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
        responseEntityWithIdentifier shouldBe ("getResponse", 1)

        maybeCollectingSpanHandler should haveSomeSpanNames(Seq("Stream", sentGet))
        maybeCollectingSpanHandler should not(haveFailedSpans)
      }

      "complete spans and add them to the Span Handler for a failed request" in {
        val eventualFailure = httpStream(unavailableHost).run flatMap {
          case (Success(response), _) => Unmarshal(response).to[String]
          case (Failure(e), _) => Future.failed(e)
        }

        Try(eventualFailure.get()) shouldBe a[Failure[_]]

        maybeCollectingSpanHandler should haveSomeSpanNames(Seq("Stream", sentGet))
        maybeCollectingSpanHandler should haveFailedSpans
      }
    }

    "sending a request with a Future based API" should {
      "create spans and add them to the Span Handler for a successful request without a body" in {
        implicit val spanContext: SpanContextWithStack = new SpanContextWithStack

        val eventualResponse = TracedHttpClient().get(localHostRootUriString)

        responseToString(eventualResponse) shouldBe "getResponse"

        maybeCollectingSpanHandler should haveSomeSpanName(sentGet)
        maybeCollectingSpanHandler should not(haveFailedSpans)
      }

      "create spans and add them to the Span Handler for a successful request with a body" in {
        implicit val spanContext: SpanContextWithStack = new SpanContextWithStack

        val eventualResponse = TracedHttpClient().get(localHostRootUriString, Some("request"))

        responseToString(eventualResponse) shouldBe "getResponse"

        maybeCollectingSpanHandler should haveSomeSpanName(sentGet)
        maybeCollectingSpanHandler should not(haveFailedSpans)
      }

      "create spans and add them to the Span Handler for a failed request" in {
        implicit val spanContext: SpanContextWithStack = new SpanContextWithStack

        val eventualFailure = TracedHttpClient().get(uri = s"http://$unavailableHost:8080/")

        Try(eventualFailure.get()) shouldBe a[Failure[_]]

        Thread.sleep(10L) // onComplete is on a separate thread and can be run after the next statements are run

        maybeCollectingSpanHandler should haveSomeSpanName(sentGet)
        maybeCollectingSpanHandler should haveFailedSpans
      }

      "create spans and add them to the Span Handler for a post request" in {
        implicit val spanContext: SpanContextWithStack = new SpanContextWithStack

        val eventualResponse = TracedHttpClient().post(localHostRootUriString, "body")

        responseToString(eventualResponse) shouldBe "postResponse"
        maybeCollectingSpanHandler should haveSomeSpanName("SENT POST /")
      }

      "create spans and add them to the Span Handler for a put request" in {
        implicit val spanContext: SpanContextWithStack = new SpanContextWithStack

        val eventualResponse = TracedHttpClient().put(localHostRootUriString, "body")

        responseToString(eventualResponse) shouldBe "putResponse"
        maybeCollectingSpanHandler should haveSomeSpanName("SENT PUT /")
      }

      "create spans and add them to the Span Handler for a head request" in {
        implicit val spanContext: SpanContextWithStack = new SpanContextWithStack

        val eventualResponse = TracedHttpClient().head(localHostRootUriString)

        eventualResponse.get() should beSuccessfulResponse

        maybeCollectingSpanHandler should haveSomeSpanName("SENT HEAD /")
      }

      "create spans and add them to the Span Handler for a patch request" in {
        implicit val spanContext: SpanContextWithStack = new SpanContextWithStack

        val eventualResponse = TracedHttpClient().patch(localHostRootUriString, "body")

        responseToString(eventualResponse) shouldBe "patchResponse"
        maybeCollectingSpanHandler should haveSomeSpanName("SENT PATCH /")
      }

      "create spans and add them to the Span Handler for a delete request" in {
        implicit val spanContext: SpanContextWithStack = new SpanContextWithStack

        val eventualResponse = TracedHttpClient().delete(localHostRootUriString)

        responseToString(eventualResponse) shouldBe "deleteResponse"
        maybeCollectingSpanHandler should haveSomeSpanName("SENT DELETE /")
      }
    }

    "sending a request should create a span that is clearly different from a Money Directive span" in {
      implicit val spanContext: SpanContextWithStack = new SpanContextWithStack

      TracedHttpClient().get(localHostRootUriString + "traced").get() should beSuccessfulResponse

      maybeCollectingSpanHandler should haveSomeSpanNames(Seq(sentGet + "traced", "RECEIVED GET /traced"))
    }

    "sending a request should use a non default span key creator if it is present" in {
      implicit val spanContext: SpanContextWithStack = new SpanContextWithStack

      val tracedRequest = "TracedRequest"
      implicit val sentSKC: SentRequestSpanKeyCreator = SentRequestSpanKeyCreator(_ => tracedRequest)

      TracedHttpClient().get(localHostRootUriString).get() should beSuccessfulResponse

      maybeCollectingSpanHandler should haveSomeSpanName(tracedRequest)
    }
  }

  val sentGet = "SENT GET /"

  val localhost = "localhost"
  val unavailableHost = "unavailableHost"

  val localHostRootUriString = s"http://$localhost:8080/"

  val route: Route =
    pathSingleSlash {
      get {
        complete("getResponse")
      } ~
        post {
          complete("postResponse")
        } ~
        put {
          complete("putResponse")
        } ~
        head {
          complete("headResponse")
        } ~
        patch {
          complete("patchResponse")
        } ~
        delete {
          complete("deleteResponse")
        }
    } ~ path("traced") {
      get {
        MoneyTrace sync {
          _ => TracedResponse(HttpResponse())
        }
      }
    }

  def haveFailedSpans: Matcher[Option[CollectingSpanHandler]] =
    Matcher {
      maybeSpanHandler =>
        val maybeSpanInfoStack = maybeSpanHandler.map(_.spanInfoStack)
        val hasSpanFailure = maybeSpanInfoStack.exists(_.map(_.success).contains(false))

        MatchResult(
          matches = hasSpanFailure,
          rawFailureMessage = s"No Spans failed in $maybeSpanInfoStack",
          rawNegatedFailureMessage = s"Spans failed in $maybeSpanInfoStack"
        )
    }

  def beSuccessfulResponse: Matcher[HttpResponse] =
    Matcher {
      response =>
        MatchResult(
          matches = response.status.isSuccess(),
          rawFailureMessage = s"HttpResponse Status Code was ${response.status} not expected successful code",
          rawNegatedFailureMessage = s"HttpResponse Status Code was ${response.status} a successful code"
        )
    }

  override def beforeAll(): Unit = Http().bindAndHandle(route, localhost, 8080)

  def httpStream(host: String = localhost): RunnableGraph[Future[(Try[HttpResponse], Int)]] =
    RunnableGraph fromGraph {
      GraphDSL.create(Sink.head[(Try[HttpResponse], Int)]) { implicit builder => sink =>
        import com.comcast.money.akka.stream.StreamTracingDSL._

        Source(List((HttpRequest(), 1))) ~|> TracedHttpClientFlow[Int](host, 8080) ~| sink.in
        ClosedShape
      }
    }

  def responseToString(eventualHttpResponse: Future[HttpResponse]): String = eventualHttpResponse.flatMap(Unmarshal(_).to[String]).get()
}
