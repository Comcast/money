package com.comcast.money.akka.stream.acceptance

import akka.actor.ActorSystem
import akka.stream.Attributes
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.stage.{InHandler, OutHandler}
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka._
import com.comcast.money.akka.stream.{TracedFlow, TracedFlowLogic}
import com.comcast.money.core.handlers.HandlerChain

class MoneyFlowSpec extends MoneyAkkaScope {

  "MoneyExtension should pass a span through an Akka Stream" in {
    implicit val spanContextWithStack: SpanContextWithStack = new SpanContextWithStack

    testStream().get()

    maybeCollectingSpanHandler should haveSomeSpanNames(testSpanNames)
  }

  "MoneyExtension should pass a span through an asynchronous Akka Stream" in {
    implicit val spanContextWithStack: SpanContextWithStack = new SpanContextWithStack

    multithreadedTestStream().get()

    maybeCollectingSpanHandler should haveSomeSpanNames(testSpanNames)
  }

  private def maybeCollectingSpanHandler: Option[CollectingSpanHandler] =
    MoneyExtension(system).handler.asInstanceOf[HandlerChain].handlers.headOption.map(_.asInstanceOf[CollectingSpanHandler])

  val testSpanNames = Seq("flow-1", "flow-2", "flow-3")

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

  class TestFlowShape(id: String, isFinalFlow: Boolean = false)
                     (implicit val actorSystem: ActorSystem) extends TracedFlow[String, String] {

    override val inletName: String = "testin"
    override val outletName: String = "testout"

    override def createLogic(inheritedAttributes: Attributes) = new TracedFlowLogic {
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val logic = (msg: String) => s"$msg$id"
          if (isFinalFlow) traceStageAndStopAll(id, logic)
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
}
