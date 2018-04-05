package com.comcast.money.akka.stream.acceptance

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.GraphDSL.Implicits.PortOps
import akka.stream.scaladsl.{Concat, Flow, GraphDSL, Partition, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, SourceShape}
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka._
import com.comcast.money.akka.stream.{AkkaMoney, TracedBuilder, TracedStreamCombinators}
import com.comcast.money.core.handlers.HandlerChain

import scala.concurrent.Future

class MoneyStreamCombinatorsSpec extends MoneyAkkaScope {

  "Tracing stream combinators" should {
    "instrument a stream" in {
      TestStreams.simple.run().get

      val maybeHandler = MoneyExtension(system).handler.asInstanceOf[HandlerChain].handlers.headOption
      val maybeSpanHandler = maybeHandler.map(_.asInstanceOf[CollectingSpanHandler])

      maybeSpanHandler should haveSomeSpanNames(Seq("Stream", "StringToString"))
    }

    "instrument a source" in {
      TestStreams.sourceEndingWithFlow.runWith(Sink.ignore).get

      val maybeHandler = MoneyExtension(system).handler.asInstanceOf[HandlerChain].handlers.headOption
      val maybeSpanHandler = maybeHandler.map(_.asInstanceOf[CollectingSpanHandler])

      maybeSpanHandler should haveSomeSpanNames(Seq("Stream", "StringToString", "StringToString"))
    }

    "instrument a stream with a fan out and fan in" in {
      TestStreams.fanOutFanIn.run().get

      val maybeHandler = MoneyExtension(system).handler.asInstanceOf[HandlerChain].handlers.headOption
      val maybeSpanHandler = maybeHandler.map(_.asInstanceOf[CollectingSpanHandler])

      maybeSpanHandler should haveSomeSpanNames(Seq("Stream", "FanInString", "StringToString", "FanOutString", "Stream", "FanInString", "StringToString", "FanOutString"))
    }
  }

  object TestStreams extends TracedStreamCombinators with AkkaMoney with TracedBuilder {
    override implicit val actorSystem: ActorSystem = _system

    private val sink = Sink.ignore

    def simple =
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

    def fanOutFanIn =
      RunnableGraph.fromGraph( GraphDSL.create(sink) {
        implicit builder: GraphDSL.Builder[Future[Done]] => sink =>

          val partitioner = (string: String) =>
            string match {
              case "chunk" => 0
              case "funk" => 1
            }

          val partition = builder.tracedAdd(Partition[String](2, partitioner))

          val concat = builder.tracedConcat(Concat[String](2))

          Source(List("chunk", "funk")) |~> partition

          partition.out(0) |~> Flow[String] |~\ concat

          partition.out(1) |~> Flow[String] |~/ concat

          concat ~| sink.in

          ClosedShape
      })
  }
}
