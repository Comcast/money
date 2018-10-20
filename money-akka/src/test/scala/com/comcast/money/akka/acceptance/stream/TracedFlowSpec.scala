/*
 * Copyright 2012 Comcast Cable Communications Management, LLC
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

import akka.stream.Attributes
import akka.stream.scaladsl.{ Keep, Sink, Source }
import akka.stream.stage.{ InHandler, OutHandler }
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka.SpanHandlerMatchers.{ haveSomeSpanNames, maybeCollectingSpanHandler }
import com.comcast.money.akka.stream.{ TracedFlow, TracedFlowLogic }
import com.comcast.money.akka.{ AkkaMoneyScope, MoneyExtension, SpanContextWithStack }
import org.scalatest.Ignore

class TracedFlowSpec extends AkkaMoneyScope {

  "MoneyExtension should pass a span through an Akka Stream" in {
    implicit val moneyExtension: MoneyExtension = MoneyExtension(actorSystem)
    implicit val spanContextWithStack: SpanContextWithStack = new SpanContextWithStack

    testStream().get()

    maybeCollectingSpanHandler should haveSomeSpanNames(testSpanNames)
  }

  "MoneyExtension should pass a span through an asynchronous Akka Stream" in {
    implicit val moneyExtension: MoneyExtension = MoneyExtension(actorSystem)
    implicit val spanContextWithStack: SpanContextWithStack = new SpanContextWithStack

    multithreadedTestStream().get()

    maybeCollectingSpanHandler should haveSomeSpanNames(testSpanNames)
  }

  val testSpanNames = Seq("flow-3", "flow-2", "flow-1")

  def testStream()(implicit spanContextWithStack: SpanContextWithStack, moneyExtension: MoneyExtension) =
    Source[(String, SpanContextWithStack)](List(("", spanContextWithStack)))
      .via(new TestFlowShape("flow-1"))
      .via(new TestFlowShape("flow-2"))
      .via(new TestFlowShape("flow-3", isFinalFlow = true))
      .runWith(Sink.seq)

  def multithreadedTestStream()(implicit spanContextWithStack: SpanContextWithStack, moneyExtension: MoneyExtension) =
    Source[(String, SpanContextWithStack)](List(("", spanContextWithStack)))
      .via(new TestFlowShape("flow-1").async)
      .via(new TestFlowShape("flow-2").async)
      .via(new TestFlowShape("flow-3", isFinalFlow = true).async)
      .runWith(Sink.seq)

  class TestFlowShape(id: String, isFinalFlow: Boolean = false)(implicit moneyExtension: MoneyExtension) extends TracedFlow[String, String] {

    override val inletName: String = "testin"
    override val outletName: String = "testout"

    override def createLogic(inheritedAttributes: Attributes) =
      new TracedFlowLogic {
        setHandler(in, new InHandler {
          override def onPush(): Unit = {
            val logic = (msg: String) => s"$msg$id"
            if (isFinalFlow) stopTracePush(key = id, stageLogic = logic)
            else tracedPush(id, logic)
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
