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

import akka.stream.scaladsl.{ Sink, Source }
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka.SpanHandlerMatchers.{ haveSomeSpanNames, maybeCollectingSpanHandler }
import com.comcast.money.akka.stream.{ PushLogic, TracedFlow }
import com.comcast.money.akka.{ AkkaMoneyScope, SpanContextWithStack, TraceContext }

class TracedFlowSpec extends AkkaMoneyScope {

  "A Akka Stream built with TracedFlow should create completed Spans in the SpanHandler" in {
    testStream(TraceContext(new SpanContextWithStack)).get()

    maybeCollectingSpanHandler should haveSomeSpanNames(testSpanNames)
  }

  "A Akka Stream built with TracedFlow should only stop Spans if it is enabled" in {
    val bothEmpty: (Seq[String], Seq[String]) => Boolean = (seq1, seq2) => seq1.isEmpty && seq2.isEmpty

    testStreamNotStoppingSpans(TraceContext(new SpanContextWithStack)).get()

    maybeCollectingSpanHandler should haveSomeSpanNames(Seq.empty, bothEmpty)
  }

  val testSpanNames = Seq("flow-3", "flow-2", "flow-1")

  def createPushLogic(id: String, shouldStop: Boolean) = PushLogic(key = id, inToOutWithIsSuccessful = (msg: String) => (s"$msg$id", true), shouldStop)

  def tracedFlow(name: String, shouldStop: Boolean = true) = TracedFlow("inlet", "outlet", createPushLogic(name, shouldStop))

  def testStream(traceContext: TraceContext) =
    Source[(String, TraceContext)](List(("", traceContext)))
      .via(tracedFlow("flow-1"))
      .via(tracedFlow("flow-2"))
      .via(tracedFlow("flow-3"))
      .runWith(Sink.ignore)

  def testStreamNotStoppingSpans(traceContext: TraceContext) =
    Source[(String, TraceContext)](List(("", traceContext)))
      .via(tracedFlow("flow-1", shouldStop = false))
      .via(tracedFlow("flow-2", shouldStop = false))
      .via(tracedFlow("flow-3", shouldStop = false))
      .runWith(Sink.ignore)
}
