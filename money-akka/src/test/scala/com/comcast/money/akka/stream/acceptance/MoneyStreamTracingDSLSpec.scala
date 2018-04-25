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

package com.comcast.money.akka.stream.acceptance

import akka.stream._
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.GraphDSL.Implicits.PortOps
import akka.stream.scaladsl.{ Balance, Broadcast, Concat, Flow, GraphDSL, Interleave, Merge, MergePrioritized, Partition, RunnableGraph, Sink, Source }
import akka.stream.stage.GraphStage
import akka.{ Done, NotUsed }
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka._
import com.comcast.money.akka.stream.DefaultSpanKeyCreators.{ DefaultFanInSpanKeyCreator, DefaultFanOutSpanKeyCreator, DefaultFlowSpanKeyCreator, DefaultSourceSpanKeyCreator }
import com.comcast.money.akka.stream._

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ ExecutionContext, Future }

class MoneyStreamTracingDSLSpec extends AkkaMoneyScope {

  implicit val executionContext: ExecutionContext = _system.dispatcher

  "A stream traced with combinators" when {
    "run should create completed spans" in {
      TestStreams.simple.run.get()

      maybeCollectingSpanHandler should haveSomeSpanNames(Seq(stream, stringToString))
    }

    "completed with an arbitrary Sink should create completed spans" in {
      TestStreams.sourceEndingWithFlow.runWith(Sink.ignore).get()

      maybeCollectingSpanHandler should haveSomeSpanNames(Seq(stream, stringToString, stringToString))
    }

    "built with a fan out and concat should create completed spans" in {
      val expectedSpanNames = replicateAndAppend(Seq(stream, fanInOfString, stringToString, fanOutOfString))

      TestStreams.fanOutFanIn().run.get()

      maybeCollectingSpanHandler should haveSomeSpanNamesInNoParticularOrder(expectedSpanNames)
    }

    "built with a fan out and fan in of a type accepted by TraceBuilder should create completed spans" in {
      val expectedSpanNames = replicateAndAppend(Seq(stream, fanInOfString, stringToString, fanOutOfString))

      val fanInShapes = List(Merge[String](2), Interleave[String](2, 1), MergePrioritized[String](Seq(1, 2)))

      fanInShapes foreach {
        fanInShape =>
          TestStreams.fanOutFanIn(fanInRaw = Right(fanInShape)).run.get()

          maybeCollectingSpanHandler should haveSomeSpanNamesInNoParticularOrder(expectedSpanNames)

          clearHandlerChain
      }
    }

    "built with a Broadcast and fan in should create completed spans" in {
      val expectedSpanNames = replicateAndAppend(Seq(stream, fanInOfString, stringToString, stringToString, fanOutOfString))

      TestStreams.fanOutFanIn(fanOutRaw = Broadcast[String](2)).run.get()

      maybeCollectingSpanHandler should haveSomeSpanNamesInNoParticularOrder(expectedSpanNames)
    }

    "built with a Balance and fan in should create completed spans" in {
      val expectedSpanNames = replicateAndAppend(Seq(stream, fanInOfString, stringToString, fanOutOfString))

      TestStreams.fanOutFanIn(fanOutRaw = Balance[String](2)).run.get()

      maybeCollectingSpanHandler should haveSomeSpanNamesInNoParticularOrder(expectedSpanNames)
    }

    "built with ordered async boundaries should run asynchronously and create completed spans" in {
      val expectedSpanNames = replicateAndAppend(Seq(stream, stringToString), 3)

      val lessThanSequentialRuntime = 750 milliseconds
      val orderedChunks = TestStreams.asyncSimple.run.get(lessThanSequentialRuntime)

      maybeCollectingSpanHandler should haveSomeSpanNames(expectedSpanNames)
      orderedChunks shouldBe Seq("chunk1", "chunk2", "chunk3")
    }

    "built with unordered async boundaries" should {
      val lessThanSequentialRuntime = 500.milliseconds

      "run out of order" in {
        val secondChunkId = Some(2)

        val orderedChunks = TestStreams.asyncOutOfOrder.run.get(lessThanSequentialRuntime)

        val maybeLastChunkToArriveId = orderedChunks.lastOption.map(_.last.asDigit)

        maybeLastChunkToArriveId should equal(secondChunkId)
      }

      "close spans for the elements they represent" in {
        TestStreams.asyncOutOfOrder.run.get(lessThanSequentialRuntime)

        val spanHandler = maybeCollectingSpanHandler.get

        val fourHundredThousandMicros = 400.milliseconds.toMicros
        val spanInfoStack = spanHandler.spanInfoStack

        val secondSpanDuration: Option[Long] = {
          val streamSpans = spanInfoStack.filter(_.name == stream).sortBy(_.startTimeMicros)
          streamSpans.tail.headOption.map(_.durationMicros)
        }

        spanInfoStack.size shouldBe 6
        secondSpanDuration.get should be > fourHundredThousandMicros
      }
    }

    "adding the key for a Span" should {
      "use the name Attribute in a Flow" in {
        TestStreams.namedFlow.run.get()

        maybeCollectingSpanHandler should haveSomeSpanNames(Seq(stream, "SomeFlowName"))
      }

      "name Stream Shapes with an unset name Attribute" in {
        val someOtherFlowName = "SomeOtherFlowName"

        implicit val fsck = FlowSpanKeyCreator((_: Flow[String, _, _]) => someOtherFlowName)

        TestStreams.namedFlow.run.get()

        maybeCollectingSpanHandler should haveSomeSpanNames(Seq(stream, someOtherFlowName))
      }

      "name Stream Shapes without a name Attribute" in {
        val someFanInName = "SomeFanInName"
        val someFanOutName = "SomeFanOutName"
        val expectedSpanNames = replicateAndAppend(Seq(stream, someFanOutName, stringToString, someFanInName))

        implicit val fanOutSKC = FanOutSpanKeyCreator[String]((_: FanOutShape[_]) => someFanOutName)

        implicit val fanInSKC = FanInSpanKeyCreator[String]((_: Inlet[_]) => someFanInName)
        TestStreams.fanOutFanIn().run.get()

        maybeCollectingSpanHandler should haveSomeSpanNamesInNoParticularOrder(expectedSpanNames)
      }

      "name Stream Shapes by the type if there are multiple implicit names for a type of Stream Shape" in {
        val overidingFlow = "OveridingFlow"
        val someStream = "SomeStream"

        implicit val fsck = FlowSpanKeyCreator((_: Flow[_, _, _]) => "SomeOtherFlowName")
        implicit val ssck = SourceSpanKeyCreator((_: Source[String, _]) => someStream)
        implicit val overidingFlowSKC = FlowSpanKeyCreator((_: Flow[String, _, _]) => overidingFlow)

        TestStreams.simple.run.get()

        maybeCollectingSpanHandler should haveSomeSpanNames(Seq(someStream, overidingFlow))
      }
    }
  }

  private def replicateAndAppend[T](seq: Seq[T], numberOfreplicas: Int = 2): Seq[T] =
    (1 to numberOfreplicas).map(_ => seq).reduce(_ ++ _)

  val stream = "Stream"
  val stringToString = "StringToString"
  val fanInOfString = "FanInOfString"
  val fanOutOfString = "FanOutOfString"

  object TestStreams {

    implicit val moneyExtension: MoneyExtension = MoneyExtension(system)

    import AsyncUnorderedFlowTracing._
    import StreamTracingDSL._
    import TracedBuilder._

    private val sink = Sink.ignore

    private def source = Source(List("chunk"))

    def simple(implicit
      fskc: FlowSpanKeyCreator[String] = DefaultFlowSpanKeyCreator[String],
      sskc: SourceSpanKeyCreator[String] = DefaultSourceSpanKeyCreator[String]) =
      RunnableGraph fromGraph {
        GraphDSL.create(sink) { implicit builder: Builder[Future[Done]] => sink =>
          source ~|> Flow[String] ~| sink.in

          ClosedShape
        }
      }

    def sourceEndingWithFlow =
      Source fromGraph {
        GraphDSL.create() {
          implicit builder =>
            val out: PortOps[String] = source ~|> Flow[String] ~|~ Flow[String]

            SourceShape(out.outlet)
        }
      }

    val partitioner: String => Int = {
      case "chunk" => 0
      case "funk" => 1
    }

    def fanOutFanIn(
      fanInRaw: Either[GraphStage[UniformFanInShape[String, String]], Graph[UniformFanInShape[String, String], NotUsed]] = Right(Concat[String](2)),
      fanOutRaw: GraphStage[UniformFanOutShape[String, String]] = Partition[String](2, partitioner)
    )(implicit
      fisck: FanInSpanKeyCreator[String] = DefaultFanInSpanKeyCreator[String],
      fosck: FanOutSpanKeyCreator[String] = DefaultFanOutSpanKeyCreator[String]) =

      RunnableGraph fromGraph {
        GraphDSL.create(sink) { implicit builder: Builder[Future[Done]] => sink =>

          val fanOut = builder.tracedAdd(fanOutRaw)

          val fanIn = fanInRaw match {
            case Right(concat) => builder.tracedConcat(concat)
            case Left(otherFanIn) => builder.tracedAdd(otherFanIn)
          }

          Source(List("chunk", "funk")) ~|> fanOut

          fanOut.out(0) ~|> Flow[String] ~<> fanIn.in(0)

          fanOut.out(1) ~|> Flow[String] ~<> fanIn.in(1)

          fanIn ~| sink.in

          ClosedShape
        }
      }

    private def stringToFuture(sleeps: (Long, Long)) =
      (string: String) =>
        Future {
          string.last.asDigit match {
            case 2 => Thread.sleep(sleeps._1)
            case 3 => Thread.sleep(sleeps._2)
            case _ =>
          }
          string
        }

    type TracedString = (String, SpanContextWithStack)

    def asyncOutOfOrder = asyncStream(builder => Right(Flow[String].tracedMapAsyncUnordered(3)(stringToFuture((400L, 200L)))))

    def asyncSimple = asyncStream(builder => Left(Flow[String].mapAsync(3)(stringToFuture(sleeps = (400L, 400L)))))

    private def asyncStream(asyncFlowCreator: TracedBuilder => Either[Flow[String, String, _], Flow[TracedString, TracedString, _]])(implicit executionContext: ExecutionContext) =
      RunnableGraph fromGraph {
        GraphDSL.create(Sink.seq[String]) { implicit builder: Builder[Future[Seq[String]]] => sink =>
          val iterator = List("chunk1", "chunk2", "chunk3").iterator
          asyncFlowCreator(builder) fold (
            asyncFlow => Source.fromIterator(() => iterator) ~|> asyncFlow ~| sink.in,
            asyncUnorderedFlow => Source.fromIterator(() => iterator) ~|> asyncUnorderedFlow ~| sink.in
          )

          ClosedShape
        }
      }

    def namedFlow(implicit fskc: FlowSpanKeyCreator[String] = DefaultFlowSpanKeyCreator[String]) =
      RunnableGraph fromGraph {
        GraphDSL.create(sink) { implicit builder: Builder[Future[Done]] => sink =>
          source ~|> Flow[String].addAttributes(Attributes(Attributes.Name("SomeFlowName"))) ~| sink.in

          ClosedShape
        }
      }
  }

}
