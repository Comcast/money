package com.comcast.money.akka.acceptance.http

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka.SpanHandlerMatchers.{haveSomeSpanNames, maybeCollectingSpanHandler}
import com.comcast.money.akka.http.{DefaultHttpRequestSpanKeyCreator, HttpRequestSpanKeyCreator}
import com.comcast.money.akka.{AkkaMoneyScope, MoneyExtension, SpanContextWithStack}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class MoneyClientSpec extends AkkaMoneyScope {

  "A Akka Http Client traced with Money" should {
    "send a traced request with a flow" in {
      val bindFuture = Http().bindAndHandle(route, "localhost", 8080)

      val eventualMaybeResponse =
        httpStream.run flatMap {
          case (Success(response), i) => Unmarshal(response).to[String].map((_, i))
          case (Failure(e), _) => Future.failed(e)
        }

      eventualMaybeResponse.get() shouldBe ("response", 1)
      bindFuture.flatMap(_.unbind()).get()

      maybeCollectingSpanHandler should haveSomeSpanNames(Seq("Stream", "GET /"))
    }
  }

  def httpStream =
    RunnableGraph fromGraph {
      GraphDSL.create(Sink.head[(Try[HttpResponse], Int)]) {
        implicit builder =>
          sink =>
            import com.comcast.money.akka.stream.StreamTracingDSL._

            Source(List((HttpRequest(), 1))) ~|> tracedHttpFlow[Int] ~| sink.in
            ClosedShape
      }
    }

  def tracedHttpFlow[T](implicit hSKC: HttpRequestSpanKeyCreator = DefaultHttpRequestSpanKeyCreator,
                        moneyExtension: MoneyExtension): Flow[((HttpRequest, T), SpanContextWithStack), ((Try[HttpResponse], T), SpanContextWithStack), _] =
    Flow[((HttpRequest, T), SpanContextWithStack)]
      .map {
        case ((request, t), spanContext) =>
          moneyExtension.tracer(spanContext).startSpan(hSKC.httpRequestToKey(request))
          (request, (t, spanContext))
      }
      .via {
        Http()
          .cachedHostConnectionPool[(T, SpanContextWithStack)](host = "localhost", port = 8080)
          .map {
            case (successHttpResponse: Success[HttpResponse], (t, spanContext)) =>
              moneyExtension.tracer(spanContext).stopSpan()
              ((successHttpResponse, t), spanContext)
            case (failed: Failure[HttpResponse], (t, spanContext)) =>
              moneyExtension.tracer(spanContext).stopSpan(result = false)
              ((failed, t), spanContext)
          }
      }

  val route =
    get {
      pathSingleSlash {
        complete("response")
      }
    }
}
