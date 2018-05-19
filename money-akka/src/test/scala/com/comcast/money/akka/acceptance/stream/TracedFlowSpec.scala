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

import akka.Done
import akka.stream.scaladsl.{ Sink, Source }
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka.SpanHandlerMatchers.{ haveFailedSpans, haveSomeSpanNames, maybeCollectingSpanHandler }
import com.comcast.money.akka.TestStreamConstants.replicateAndAppend
import com.comcast.money.akka.stream._
import com.comcast.money.akka.{ AkkaMoneyScope, TraceContext }

import scala.concurrent.Future

class TracedFlowSpec extends AkkaMoneyScope {

  "A Akka Stream built with TracedFlow" should {

    "create completed Spans in the SpanHandler" in {
      testStream(TraceContext()).get()

      maybeCollectingSpanHandler should haveSomeSpanNames(testSpanNames)
    }

    "only stop Spans if it is enabled" in {
      val bothEmpty: (Seq[String], Seq[String]) => Boolean = (seq1, seq2) => seq1.isEmpty && seq2.isEmpty

      testStreamNotStoppingSpans(TraceContext()).get()

      maybeCollectingSpanHandler should haveSomeSpanNames(Seq.empty, bothEmpty)
    }

    "maintain state if required" in {
      val collectedStreamElements = (1 to 3).map(_ => "flow-1")

      testStreamWithStatefulFlows(TraceContext()).get()

      SimpleStatefulPusher.elements shouldBe collectedStreamElements
    }

    "not pass down the stream if the logic returns Unit" in {
      val spanNamesWithElementNotReachingThirdFLow = replicateAndAppend(Seq(unitFlowName, "flow-1"), 3)

      testStreamWithUnitFlow(TraceContext()).get()

      maybeCollectingSpanHandler should haveSomeSpanNames(spanNamesWithElementNotReachingThirdFLow)
      maybeCollectingSpanHandler should haveFailedSpans
    }
  }

  val testSpanNames = Seq("flow-3", "flow-2", "flow-1")

  def createPushLogic(id: String, usingTracingDSL: TracingDSLUsage) = StatelessPushConfig(key = id, pushLogic = (msg: String) => (Right(s"$msg$id"), true), usingTracingDSL)

  def tracedFlow(name: String, usingTracingDSL: TracingDSLUsage = NotUsingTracingDSL) = TracedFlow(createPushLogic(name, usingTracingDSL))

  def testStream(traceContext: TraceContext): Future[Done] =
    Source[(String, TraceContext)](List(("", traceContext)))
      .via(tracedFlow("flow-1"))
      .via(tracedFlow("flow-2"))
      .via(tracedFlow("flow-3"))
      .runWith(Sink.ignore)

  def testStreamNotStoppingSpans(traceContext: TraceContext): Future[Done] =
    Source[(String, TraceContext)](List(("", traceContext)))
      .via(tracedFlow("flow-1", UsingTracingDSL))
      .via(tracedFlow("flow-2", UsingTracingDSL))
      .via(tracedFlow("flow-3", UsingTracingDSL))
      .runWith(Sink.ignore)

  object SimpleStatefulPusher extends StatefulPusher[String, String] {
    var elements: Seq[String] = Seq.empty

    override def push(in: String): (Either[Unit, String], Boolean) = {
      elements = elements :+ in
      (Right(in), true)
    }
  }

  def testStreamWithStatefulFlows(traceContext: TraceContext): Future[Done] =
    Source[(String, TraceContext)]((1 to 3).map(_ => ("", traceContext)))
      .via(tracedFlow("flow-1"))
      .via(statefulFlow("flow-2"))
      .via(tracedFlow("flow-3"))
      .runWith(Sink.ignore)

  def testStreamWithUnitFlow(traceContext: TraceContext): Future[Done] =
    Source[(String, TraceContext)]((1 to 3).map(_ => ("", traceContext)))
      .via(tracedFlow("flow-1"))
      .via(unitFlow("flow-2"))
      .via(tracedFlow("flow-3"))
      .runWith(Sink.ignore)

  private val unitFlowName = "UnitFlow"
  private def unitFlow(key: String): TracedFlow[String, String] = TracedFlow(StatelessPushConfig(unitFlowName, _ => (Left(Unit), false), NotUsingTracingDSL))

  private def statefulFlow(key: String): TracedFlow[String, String] = TracedFlow(StatefulPushConfig(key, SimpleStatefulPusher, NotUsingTracingDSL))
}
