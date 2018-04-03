package com.comcast.money.akka.stream.acceptance

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.GraphDSL.Implicits.PortOps
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, SourceShape}
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka._
import com.comcast.money.akka.stream.{AkkaMoney, TracingStreamCombinators}
import com.comcast.money.core.handlers.HandlerChain

import scala.concurrent.Future

class MoneyStreamCombinatorsSpec extends MoneyAkkaScope {

  "Tracing stream combinators" should {
    "instrument a stream" in {
      TestStream.simple().run().get

      val maybeHandler = MoneyExtension(system).handler.asInstanceOf[HandlerChain].handlers.headOption
      val maybeSpanHandler = maybeHandler.map(_.asInstanceOf[CollectingSpanHandler])

      maybeSpanHandler should haveSomeSpanNames(Seq("SourceOfString", "StringToString"))
    }

    "instrument a source" in {
      TestStream.sourceEndingWithFlow.runWith(Sink.ignore).get

      val maybeHandler = MoneyExtension(system).handler.asInstanceOf[HandlerChain].handlers.headOption
      val maybeSpanHandler = maybeHandler.map(_.asInstanceOf[CollectingSpanHandler])

      maybeSpanHandler should haveSomeSpanNames(Seq("SourceOfString", "StringToString", "StringToString"))
    }
  }

  object TestStream extends TracingStreamCombinators with AkkaMoney {
    override implicit val actorSystem: ActorSystem = _system

    val sink = Sink.ignore

    def simple() =
      RunnableGraph.fromGraph(GraphDSL.create(sink) {
        implicit builder: GraphDSL.Builder[Future[Done]] => sink =>

          (Source(List("chunk")) |~> Flow[String]) ~| sink.in

          ClosedShape
      })

    def sourceEndingWithFlow =
      Source.fromGraph(GraphDSL.create() {
        implicit builder =>
          val out: PortOps[String] = (Source(List("chunk")) |~> Flow[String]) ~>| Flow[String]

          SourceShape(out.outlet)
      })
  }
}
