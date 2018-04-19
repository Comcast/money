package com.comcast.money.akka

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.comcast.money.core.handlers.HandlerChain
import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.matchers.{MatchResult, Matcher}

class MoneyDirectiveSpec(override val system: ActorSystem) extends TestKit(system) with WordSpecLike with Matchers with ScalatestRouteTest with BeforeAndAfterEach {

  def this() = this {
    val configString: String =
      """
        | money {
        |  handling = {
        |    async = false
        |    handlers = [
        |       {
        |         class = "com.comcast.money.akka.CollectingSpanHandler"
        |         log-level = "INFO"
        |       }
        |    ]
        |  }
        | }""".stripMargin

    ActorSystem("MoneyExtensionSpec", ConfigFactory.parseString(configString))
  }

  implicit val actorSystem = system

  implicit val matierializer: ActorMaterializer = ActorMaterializer()

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }

  val testRoute =
    pathSingleSlash {
      get {
        Trace {
          (_, spanContext) => (HttpResponse(entity = "response"), spanContext)
        }
      }
    }

  override def beforeEach(): Unit =
    MoneyExtension(system)
      .handler
      .asInstanceOf[HandlerChain]
      .handlers
      .headOption
      .foreach(_.asInstanceOf[CollectingSpanHandler].clear())

  def maybeCollectingSpanHandler =
    MoneyExtension(system)
      .handler
      .asInstanceOf[HandlerChain]
      .handlers
      .headOption
      .map(_.asInstanceOf[CollectingSpanHandler])

  "A Akka Http route with a MoneyDirective" should {
    "start a span for a request" in {
      Get("/") ~> testRoute ~> check(responseAs[String] shouldBe "response")

      maybeCollectingSpanHandler should haveSomeSpanNames(Seq("HttpRequest"))
    }
  }

  def haveSomeSpanNames(expectedSpanNames: Seq[String]) =
    Matcher {
      (maybeSpanHandler: Option[CollectingSpanHandler]) =>
        val maybeNames = maybeSpanHandler.map(_.spanInfoStack.map(_.name()))

        def checkNames(names: Seq[String]): Boolean =
          names
            .zip(expectedSpanNames)
            .map { case (expectedName, actualName) => expectedName == actualName }
            .reduce(_ == _)

        MatchResult(
          matches = {
            maybeNames match {
              case Some(spanNames) if spanNames.isEmpty => false
              case Some(spanNames) if checkNames(spanNames) => true
              case _ => false
            }
          },
          rawFailureMessage = s"Names: $maybeNames were not Some($expectedSpanNames)",
          rawNegatedFailureMessage = s"Names: $maybeNames were Some($expectedSpanNames)"
        )
    }

  object Trace {
    def apply(f: (HttpRequest, SpanContextWithStack) ⇒ (HttpResponse, SpanContextWithStack))(implicit actorSystem: ActorSystem) =
      extractRequest {
        request =>
          val moneyExtension = MoneyExtension(actorSystem)
          val spanContext: SpanContextWithStack = new SpanContextWithStack
          moneyExtension.tracer(spanContext).startSpan("HttpRequest")
          val (response, spanContextResult) = f(request, spanContext)
          moneyExtension.tracer(spanContextResult).stopSpan()
          complete(response)
      }
  }

}
