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
    "send a traced request" in {
      Http().bindAndHandle(route, "localhost", 8080)

      val eventualMaybeResponse =
        httpStream.run flatMap {
          case Success(response) => Unmarshal(response).to[String]
          case Failure(e) => Future.failed(e)
        }

      eventualMaybeResponse.get() shouldBe "response"

      maybeCollectingSpanHandler should haveSomeSpanNames(Seq("Stream", "GET /"))
    }
  }

  def httpStream: RunnableGraph[Future[Try[HttpResponse]]] =
    RunnableGraph fromGraph {
      GraphDSL.create(Sink.head[Try[HttpResponse]]) {
        implicit builder =>
          sink =>
            import com.comcast.money.akka.stream.StreamTracingDSL._

            Source(List(HttpRequest())) ~|> tracedHttpFlow ~| sink.in
            ClosedShape
      }
    }

  def tracedHttpFlow(implicit hSKC: HttpRequestSpanKeyCreator = DefaultHttpRequestSpanKeyCreator,
                     moneyExtension: MoneyExtension): Flow[(HttpRequest, SpanContextWithStack), (Try[HttpResponse], SpanContextWithStack), _] =
    Flow[(HttpRequest, SpanContextWithStack)]
      .map {
        case (request, spanContext) =>
          moneyExtension.tracer(spanContext).startSpan(hSKC.httpRequestToKey(request))
          (request, spanContext)
      }
      .via {
        Http()
          .cachedHostConnectionPool[SpanContextWithStack]("localhost", port = 8080)
          .map {
            case (successHttpResponse: Success[HttpResponse], spanContext) =>
              moneyExtension.tracer(spanContext).stopSpan()
              (successHttpResponse, spanContext)
            case (failed: Failure[HttpResponse], spanContext) =>
              moneyExtension.tracer(spanContext).stopSpan(result = false)
              (failed, spanContext)
          }
      }

  val route =
    get {
      pathSingleSlash {
        complete("response")
      }
    }
}
