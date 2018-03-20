package com.comcast.money.akka

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.stage.{InHandler, OutHandler}
import akka.testkit.TestKit
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.api._
import com.comcast.money.core.handlers.HandlerChain
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}

class MoneyExtensionSpec(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with BeforeAndAfterAll with BeforeAndAfterEach with Matchers {

  def this() = this {
    val configString: String =
      """
        | money {
        |  handling = {
        |    async = false
        |    handlers = [
        |    {
        |      class = "com.comcast.money.akka.CollectingSpanHandler"
        |      log-level = "INFO"
        |    }]
        |  }
        | }""".stripMargin

    ActorSystem("MoneyExtensionSpec", ConfigFactory.parseString(configString))
  }

  implicit val matierializer: ActorMaterializer = ActorMaterializer()

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  override def beforeEach(): Unit =
    MoneyExtension(system)
      .handler
      .asInstanceOf[HandlerChain]
      .handlers
      .headOption
      .foreach(_.asInstanceOf[CollectingSpanHandler].clear())

  "MoneyExtension should pass a span through an Akka Stream" in {
    implicit val spanContextWithStack: SpanContextWithStack = new SpanContextWithStack

    testStream().get

    val maybeHandler = MoneyExtension(system).handler.asInstanceOf[HandlerChain].handlers.headOption
    val maybeCollectingSpanHandler = maybeHandler.map(_.asInstanceOf[CollectingSpanHandler])

    maybeCollectingSpanHandler should haveSomeSpanNames(testSpanNames)
  }

  "MoneyExtension should pass a span through an asynchronous Akka Stream" in {
    implicit val spanContextWithStack: SpanContextWithStack = new SpanContextWithStack

    multithreadedTestStream().get

    val maybeHandler = MoneyExtension(system).handler.asInstanceOf[HandlerChain].handlers.headOption
    val maybeCollectingSpanHandler = maybeHandler.map(_.asInstanceOf[CollectingSpanHandler])

    maybeCollectingSpanHandler should haveSomeSpanNames(testSpanNames)
  }

  val testSpanNames = Seq("flow-1", "flow-2", "flow-3")

  def haveSomeSpanNames(expectedSpanNames: Seq[String]) =
    Matcher { (maybeSpanHandler: Option[CollectingSpanHandler]) =>
      val maybeNames = maybeSpanHandler.map(_.spanInfoStack.map(_.name()))

      def checkNames(names: Seq[String]): Boolean =
        names
          .zip(expectedSpanNames)
          .map { case (expectedName, actualName) => expectedName == actualName }
          .reduce(_ == _)

      MatchResult(
        matches = {
          maybeNames match {
            case Some(spanNames) if checkNames(spanNames) =>
              true
            case _ => false
          }
        },
        rawFailureMessage = s"Names: $maybeNames were not Some($expectedSpanNames)",
        rawNegatedFailureMessage = s"Names: $maybeNames were Some($expectedSpanNames)"
      )
    }

  def testStream()(implicit spanContextWithStack: SpanContextWithStack) =
    Source[(String, SpanContextWithStack)](List(("", spanContextWithStack)))
      .via(new TestFlowShape("flow-1"))
      .via(new TestFlowShape("flow-2"))
      .via(new TestFlowShape("flow-3", isFinalFlow = true))
      .toMat(Sink.seq)(Keep.right)
      .run()

  def multithreadedTestStream()(implicit spanContextWithStack: SpanContextWithStack) =
    Source[(String, SpanContextWithStack)](List(("", spanContextWithStack)))
      .via(new TestFlowShape("flow-1").async)
      .via(new TestFlowShape("flow-2").async)
      .via(new TestFlowShape("flow-3", isFinalFlow = true).async)
      .toMat(Sink.seq)(Keep.right)
      .run()
}

class TestFlowShape(id: String, isFinalFlow: Boolean = false)
                   (implicit val actorSystem: ActorSystem) extends TracedFlow[String, String] {

  override val inletName: String = "testin"
  override val outletName: String = "testout"

  override def createLogic(inheritedAttributes: Attributes) = new TracedLogic[String, String](shape) {
    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val logic = (msg: String) => s"$msg$id"
        if (isFinalFlow) traceStageAndStop(id, logic)
        else traceStageAndPush(id, logic)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit =
        if (isClosed(in)) completeStage()
        else pull(in)
    })
  }
}

class CollectingSpanHandler() extends SpanHandler {
  var spanInfoStack = Seq.empty[SpanInfo]

  def push(span: SpanInfo): Unit = spanInfoStack = span +: spanInfoStack

  def pop: Option[SpanInfo] = spanInfoStack.headOption

  def clear(): Unit = spanInfoStack = Seq.empty[SpanInfo]

  override def handle(span: SpanInfo): Unit = push(span)
}

object Blocking {

  implicit class RichFuture[T](future: Future[T]) {
    def get: T = Await.result(future, 5 seconds)
  }

}
