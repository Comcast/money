package com.comcast.money.akka.stream.acceptance

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.GraphDSL.Implicits.PortOps
import akka.stream.scaladsl.{Concat, Flow, GraphDSL, Partition, RunnableGraph, Sink, Source}
import akka.stream.{Attributes, ClosedShape, SourceShape}
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka._
import com.comcast.money.akka.stream._
import com.comcast.money.core.handlers.HandlerChain

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

class MoneyStreamCombinatorsSpec extends MoneyAkkaScope {

  implicit val executionContext: ExecutionContext = _system.dispatcher

  "Tracing stream combinators" should {
    "instrument a stream" in {
      TestStreams.simple.run().get()

      val maybeHandler = MoneyExtension(system).handler.asInstanceOf[HandlerChain].handlers.headOption
      val maybeSpanHandler = maybeHandler.map(_.asInstanceOf[CollectingSpanHandler])

      maybeSpanHandler should haveSomeSpanNames(Seq(stream, stringToString))
    }

    "instrument a source" in {
      TestStreams.sourceEndingWithFlow.runWith(Sink.ignore).get()

      val maybeHandler = MoneyExtension(system).handler.asInstanceOf[HandlerChain].handlers.headOption
      val maybeSpanHandler = maybeHandler.map(_.asInstanceOf[CollectingSpanHandler])

      maybeSpanHandler should haveSomeSpanNames(Seq(stream, stringToString, stringToString))
    }

    "instrument a stream with a fan out and fan in" in {
      val expectedSpanNames = replicateAndAppend(Seq(stream, "FanInString", stringToString, "FanOutString"))

      TestStreams.fanOutFanInWithConcat.run().get()

      val maybeHandler = MoneyExtension(system).handler.asInstanceOf[HandlerChain].handlers.headOption
      val maybeSpanHandler = maybeHandler.map(_.asInstanceOf[CollectingSpanHandler])

      maybeSpanHandler should haveSomeSpanNames(expectedSpanNames)
    }

    "allow instrumented streams with ordered async boundaries to run asynchronously" in {
      val expectedSpanNames = replicateAndAppend(Seq(stream, "StringtoString"))

      val orderedChunks = TestStreams.async.run().get(500 milliseconds)

      val maybeHandler = MoneyExtension(system).handler.asInstanceOf[HandlerChain].handlers.headOption
      val maybeSpanHandler = maybeHandler.map(_.asInstanceOf[CollectingSpanHandler])

      maybeSpanHandler should haveSomeSpanNames(expectedSpanNames)
      orderedChunks shouldBe Seq("chunk1", "chunk2", "chunk3")
    }

    "name a Span after the name of the flow" in {
      val expectedSpanNames = Seq(stream, "SomeFlowName")

      TestStreams.namedFlow.run().get()

      val maybeHandler = MoneyExtension(system).handler.asInstanceOf[HandlerChain].handlers.headOption
      val maybeSpanHandler = maybeHandler.map(_.asInstanceOf[CollectingSpanHandler])

      maybeSpanHandler should haveSomeSpanNames(expectedSpanNames)
    }
  }

  private def replicateAndAppend[T](seq: Seq[T], numberOfreplicas: Int = 2): Seq[T] =
    (1 to numberOfreplicas).map(_ => seq).reduce(_ ++ _)

  val stream = "Stream"
  val stringToString = "StringToString"

  object TestStreams extends TracedStreamCombinators with AkkaMoney with TracedBuilder {
    override implicit val actorSystem: ActorSystem = _system

    private val sink = Sink.ignore

    private def source = Source(List("chunk"))

    def simple =
      RunnableGraph.fromGraph(GraphDSL.create(sink) {
        implicit builder: Builder[Future[Done]] =>
          sink =>

            (source |~> Flow[String]) ~| sink.in

            ClosedShape
      })

    def sourceEndingWithFlow =
      Source.fromGraph(GraphDSL.create() {
        implicit builder =>
          val out: PortOps[String] = (source |~> Flow[String]) ~|> Flow[String]

          SourceShape(out.outlet)
      })

    def fanOutFanInWithConcat =
      RunnableGraph.fromGraph(GraphDSL.create(sink) {
        implicit builder: Builder[Future[Done]] =>
          sink =>

            val partitioner =
              (string: String) =>
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

    def async(implicit executionContext: ExecutionContext) =
      RunnableGraph.fromGraph(GraphDSL.create(Sink.seq[String].async) {
        implicit builder: Builder[Future[Seq[String]]] =>
          sink =>
            val stringToFuture =
              (string: String) =>
                Future {
                  string.last.asDigit match {
                    case 2 => Thread.sleep(400)
                    case 3 => Thread.sleep(400)
                    case _ =>
                  }
                  string
                }

            val iterator = List("chunk1", "chunk2", "chunk3").iterator
            (Source.fromIterator(() => iterator) |~> Flow[String].mapAsync(3)(stringToFuture)) ~| sink.in

            ClosedShape
      })

    def namedFlow =
      RunnableGraph.fromGraph {
        GraphDSL.create(sink) {
          implicit builder: Builder[Future[Done]] =>
            sink =>

              (source |~> Flow[String].addAttributes(Attributes(Attributes.Name("SomeFlowName")))) ~| sink.in

              ClosedShape
        }
      }
  }
}
