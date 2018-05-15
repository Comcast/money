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

package com.comcast.money.akka.acceptance.stream

import akka.stream._
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka.SpanHandlerMatchers.{ haveSomeSpanNames, haveSomeSpanNamesInNoParticularOrder, maybeCollectingSpanHandler }
import com.comcast.money.akka._
import com.comcast.money.akka.stream._

import scala.concurrent.duration.DurationDouble

class StreamTracingDSLSpec extends AkkaMoneyScope {

  val testStreams = new TestStreams

  import TestStreamConstants._

  "A stream traced with combinators" when {
    "run should create completed spans" in {
      testStreams.simple.run.get()

      maybeCollectingSpanHandler should haveSomeSpanNames(Seq(stream, stringToString))
    }

    "completed with an arbitrary Sink should create completed spans" in {
      testStreams.sourceEndingWithFlow.runWith(Sink.ignore).get()

      maybeCollectingSpanHandler should haveSomeSpanNames(Seq(stream, stringToString, stringToString))
    }

    "built with a fan out and fan in should create completed spans" in {
      val expectedSpanNames = replicateAndAppend(Seq(stream, fanInOfString, stringToString, fanOutOfString))

      testStreams.fanOutFanIn().run.get()

      maybeCollectingSpanHandler should haveSomeSpanNamesInNoParticularOrder(expectedSpanNames)
    }

    "built with inlets and outlets should create completed spans" in {
      testStreams.simpleWithInlets.run.get()

      maybeCollectingSpanHandler should haveSomeSpanNames(Seq(stream, "InletOfString", stringToString))
    }

    "a SpanContext is in scope should use that SpanContext" in {
      implicit val spanContextWithStack = new SpanContextWithStack
      moneyExtension.tracer.startSpan("request")

      testStreams.simpleTakingSpanContext.run.get()

      moneyExtension.tracer.stopSpan()

      maybeCollectingSpanHandler should haveSomeSpanNames(Seq("request", stream, stringToString))
    }

    "built with ordered async boundaries" should {
      "run asynchronously and create completed spans" in {
        val expectedSpanNames = replicateAndAppend(Seq(stream, stringToString), 3)

        val lessThanSequentialRuntime = 750 milliseconds
        val orderedChunks = testStreams.asyncSimple.run.get(lessThanSequentialRuntime)

        maybeCollectingSpanHandler should haveSomeSpanNames(expectedSpanNames)
        orderedChunks shouldBe Seq("chunk1", "chunk2", "chunk3")
      }

      "run asynchronously and create completed spans using a function other than map" in {
        val expectedSpanNames = replicateAndAppend(Seq(stream, stringToString), 3)

        val lessThanSequentialRuntime = 750 milliseconds
        val orderedChunks = testStreams.asyncScan.run.get(lessThanSequentialRuntime)

        maybeCollectingSpanHandler should haveSomeSpanNames(expectedSpanNames)
        orderedChunks shouldBe Seq("", "1", "12")
      }
    }

    "built with unordered async boundaries" should {
      val lessThanSequentialRuntime = 500.milliseconds

      "run out of order" in {
        val someSecondChunkId = Some(2)

        val orderedChunks = testStreams.asyncOutOfOrder.run.get(lessThanSequentialRuntime)

        val maybeLastChunkToArriveId = orderedChunks.lastOption.map(_.last.asDigit)

        maybeLastChunkToArriveId shouldBe someSecondChunkId
      }

      "close spans for the elements they represent" in {
        testStreams.asyncOutOfOrder.run.get(lessThanSequentialRuntime)

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
        testStreams.namedFlow.run.get()

        maybeCollectingSpanHandler should haveSomeSpanNames(Seq(stream, "SomeFlowName"))
      }

      "name Stream Shapes with an unset name Attribute" in {
        val someOtherFlowName = "SomeOtherFlowName"

        implicit val fsck = FlowSpanKeyCreator((_: Flow[String, _, _]) => someOtherFlowName)

        testStreams.namedFlow.run.get()

        maybeCollectingSpanHandler should haveSomeSpanNames(Seq(stream, someOtherFlowName))
      }

      "name Stream Shapes without a name Attribute" in {
        val someFanInName = "SomeFanInName"
        val someFanOutName = "SomeFanOutName"
        val expectedSpanNames = replicateAndAppend(Seq(stream, someFanOutName, stringToString, someFanInName))

        implicit val fanOutSKC = FanOutSpanKeyCreator[String]((_: FanOutShape[_]) => someFanOutName)

        implicit val fanInSKC = FanInSpanKeyCreator[String]((_: Inlet[_]) => someFanInName)
        testStreams.fanOutFanIn().run.get()

        maybeCollectingSpanHandler should haveSomeSpanNamesInNoParticularOrder(expectedSpanNames)
      }

      "name a Inlet" in {
        val someInlet = "SomeInlet"
        implicit val inletSKC = InletSpanKeyCreator[String]((_: Inlet[_]) => someInlet)

        testStreams.simpleWithInlets.run.get()

        maybeCollectingSpanHandler should haveSomeSpanNames(Seq(stream, someInlet, stringToString))
      }

      "name Stream Shapes by the type if there are multiple implicit names for a type of Stream Shape" in {
        val overidingFlow = "OveridingFlow"
        val someStream = "SomeStream"

        implicit val fsck = FlowSpanKeyCreator((_: Flow[_, _, _]) => "SomeOtherFlowName")
        implicit val ssck = SourceSpanKeyCreator((_: Source[String, _]) => someStream)
        implicit val overidingFlowSKC = FlowSpanKeyCreator((_: Flow[String, _, _]) => overidingFlow)

        testStreams.simple.run.get()

        maybeCollectingSpanHandler should haveSomeSpanNames(Seq(someStream, overidingFlow))
      }
    }
  }

}
