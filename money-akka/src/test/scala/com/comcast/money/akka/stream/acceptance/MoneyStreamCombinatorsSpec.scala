package com.comcast.money.akka.stream.acceptance

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.GraphDSL.Implicits.PortOps
import akka.stream.scaladsl.{Concat, Flow, GraphDSL, Partition, RunnableGraph, Sink, Source}
import akka.stream.{Attributes, ClosedShape, SourceShape}
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka._
import com.comcast.money.akka.stream.DefaultSpanKeyCreators.{DefaultFanInSpanKeyCreator, DefaultFanOutSpanKeyCreator}
import com.comcast.money.akka.stream._
import com.comcast.money.core.handlers.HandlerChain

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

class MoneyStreamCombinatorsSpec extends MoneyAkkaScope {

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

    "built with a fan out and fan in should create completed spans" in {
      val expectedSpanNames = replicateAndAppend(Seq(stream, "FanInOfString", stringToString, "FanOutOfString"))

      TestStreams.fanOutFanInWithConcat.run.get()

      maybeCollectingSpanHandler should haveSomeSpanNames(expectedSpanNames)
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
        implicit val fskc: FlowSpanKeyCreator = DefaultSpanKeyCreators.DefaultFlowSpanKeyCreator

        TestStreams.namedFlow.run.get()

        maybeCollectingSpanHandler should haveSomeSpanNames(Seq(stream, "SomeFlowName"))
      }

      "name Stream Shapes with an unset name Attribute" in {
        val someOtherFlowName = "SomeOtherFlowName"

        implicit val fsck = FlowSpanKeyCreator(_ => someOtherFlowName)

        TestStreams.namedFlow.run.get()

        maybeCollectingSpanHandler should haveSomeSpanNames(Seq(stream, someOtherFlowName))
      }

      "name Stream Shapes without a name Attribute" in {
        val someFanInName = "SomeFanInName"
        val someFanOutName = "SomeFanOutName"

        implicit val fanOutSKC = FanOutSpanKeyCreator(_ => someFanOutName)
        implicit val fanInSKC = FanInSpanKeyCreator(_ => someFanInName)

        TestStreams.fanOutFanInWithConcat.run.get()

        maybeCollectingSpanHandler should haveSomeSpanNames(Seq(stream, someFanOutName, stringToString, someFanInName))
      }
    }
  }

  private def maybeCollectingSpanHandler =
    MoneyExtension(system)
      .handler
      .asInstanceOf[HandlerChain]
      .handlers
      .headOption
      .map(_.asInstanceOf[CollectingSpanHandler])

  private def replicateAndAppend[T](seq: Seq[T], numberOfreplicas: Int = 2): Seq[T] =
    (1 to numberOfreplicas).map(_ => seq).reduce(_ ++ _)

  val stream = "Stream"
  val stringToString = "StringToString"

  object TestStreams extends TracedStreamCombinators with AkkaMoney with TracedBuilder {
    override implicit val actorSystem: ActorSystem = _system

    private val sink = Sink.ignore

    private def source = Source(List("chunk"))

    def simple =
      RunnableGraph fromGraph {
        GraphDSL.create(sink) {
          implicit builder: Builder[Future[Done]] =>
            sink =>
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

    def fanOutFanInWithConcat(implicit fisck: FanInSpanKeyCreator = DefaultFanInSpanKeyCreator,
                              fosck: FanOutSpanKeyCreator = DefaultFanOutSpanKeyCreator) =
      RunnableGraph fromGraph {
        GraphDSL.create(sink) {
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

              Source(List("chunk", "funk")) ~|> partition

              partition.out(0) ~|> Flow[String] ~<> concat.in(0)

              partition.out(1) ~|> Flow[String] ~<> concat.in(1)

              concat ~| sink.in

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

    private def asyncStream(asyncFlowCreator: TracedBuilder => Either[Flow[String, String, _], Flow[TracedString, TracedString, _]])
                                      (implicit executionContext: ExecutionContext) =
      RunnableGraph fromGraph {
        GraphDSL.create(Sink.seq[String]) {
          implicit builder: Builder[Future[Seq[String]]] =>
            sink =>
              val iterator = List("chunk1", "chunk2", "chunk3").iterator
              asyncFlowCreator(builder) fold (
                asyncFlow => Source.fromIterator(() => iterator) ~|> asyncFlow ~| sink.in,
                asyncUnorderedFlow => Source.fromIterator(() => iterator) ~|> asyncUnorderedFlow ~| sink.in
              )

              ClosedShape
        }
      }

    def namedFlow(implicit fskc: FlowSpanKeyCreator) =
      RunnableGraph fromGraph {
        GraphDSL.create(sink) {
          implicit builder: Builder[Future[Done]] =>
            sink =>
              source ~|> Flow[String].addAttributes(Attributes(Attributes.Name("SomeFlowName"))) ~| sink.in

              ClosedShape
        }
      }
  }
}
