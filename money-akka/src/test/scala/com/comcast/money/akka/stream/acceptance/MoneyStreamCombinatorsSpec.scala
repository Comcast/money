package com.comcast.money.akka.stream.acceptance

import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka._
import com.comcast.money.akka.stream.{AkkaMoney, TracingStreamCombinators}
import com.comcast.money.core.handlers.HandlerChain

import scala.concurrent.Future

class MoneyStreamCombinatorsSpec extends MoneyAkkaScope {

  "StreamCombinators" should {
    "instrument a flow that does not take SpanContexts" in {
      implicit val spanContextWithStack: SpanContextWithStack = new SpanContextWithStack

      TestStream().run().get

      val maybeHandler = MoneyExtension(system).handler.asInstanceOf[HandlerChain].handlers.headOption
      val maybeSpanHandler = maybeHandler.map(_.asInstanceOf[CollectingSpanHandler])

      maybeSpanHandler should haveSomeSpanNames(Seq("SourceOfString", "StringToString"))
    }
  }

  object TestStream extends TracingStreamCombinators with AkkaMoney {
    override implicit val actorSystem: ActorSystem = _system

    val sink = Sink.seq[String]

    def apply() =
      RunnableGraph.fromGraph(GraphDSL.create(sink) {
        implicit builder: GraphDSL.Builder[Future[Seq[String]]] => sink =>
          (Source(List("chunk")) |~> Flow[String]) ~|> sink.in

          ClosedShape
      })
  }
}
