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

import akka.stream.scaladsl.{ Balance, Broadcast, Merge, MergePrioritized }
import akka.stream.stage.{ GraphStage, GraphStageLogic }
import akka.stream.{ Attributes, UniformFanInShape, UniformFanOutShape }
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka.SpanHandlerMatchers.{ haveSomeSpanNamesInNoParticularOrder, maybeCollectingSpanHandler }
import com.comcast.money.akka.stream.{ UnsupportedUniformFanInShape, UnsupportedUniformFanOutShape }
import com.comcast.money.akka.{ AkkaMoneyScope, TestStreams }

class TracedBuilderSpec extends AkkaMoneyScope {

  import com.comcast.money.akka.TestStreamConstants._

  val testStreams = new TestStreams

  "With TracedBuilder in scope a Graph Stage Builder" should {
    "create a traced version of a Merge shape and produce completed spans" in {
      val expectedSpanNames = replicateAndAppend(Seq(stream, fanInOfString, stringToString, fanOutOfString))

      testStreams.fanOutFanIn(_.tracedAdd(Merge[String](2))).run.get()

      maybeCollectingSpanHandler should haveSomeSpanNamesInNoParticularOrder(expectedSpanNames)
    }

    "create a traced version of a Interleave shape and produce completed spans" in {
      val expectedSpanNames = replicateAndAppend(Seq(stream, fanInOfString, stringToString, fanOutOfString))

      testStreams.fanOutFanIn(_.tracedInterleave(2, 1)).run.get()

      maybeCollectingSpanHandler should haveSomeSpanNamesInNoParticularOrder(expectedSpanNames)
    }

    "create a traced version of a MergePrioritized shape and produce completed spans" in {
      val expectedSpanNames = replicateAndAppend(Seq(stream, fanInOfString, stringToString, fanOutOfString))

      testStreams.fanOutFanIn(_.tracedAdd(MergePrioritized[String](Seq(1, 2)))).run.get()

      maybeCollectingSpanHandler should haveSomeSpanNamesInNoParticularOrder(expectedSpanNames)
    }

    "fail with an exception when asked to create a traced version of an unsupported fan in shape" in {
      intercept[UnsupportedUniformFanInShape] {
        testStreams.fanOutFanIn(_.tracedAdd(UnsupportedFanIn))
      }
    }

    "create a traced version of a Broadcast shape and produce completed spans" in {
      val expectedSpanNames = replicateAndAppend(Seq(stream, fanInOfString, fanInOfString, stringToString, stringToString, fanOutOfString))

      testStreams.fanOutFanIn(fanOutRaw = Broadcast[String](2)).run.get()

      maybeCollectingSpanHandler should haveSomeSpanNamesInNoParticularOrder(expectedSpanNames)
    }

    "create a traced version of a Balance shape and produce completed spans" in {
      val expectedSpanNames = replicateAndAppend(Seq(stream, fanInOfString, stringToString, fanOutOfString))

      testStreams.fanOutFanIn(fanOutRaw = Balance[String](2)).run.get()

      maybeCollectingSpanHandler should haveSomeSpanNamesInNoParticularOrder(expectedSpanNames)
    }

    "fail with an exception when asked to create a traced version of an unsupported fan out shape" in {
      intercept[UnsupportedUniformFanOutShape] {
        testStreams.fanOutFanIn(fanOutRaw = UnsupportedFanOut)
      }
    }
  }

  object UnsupportedFanIn extends GraphStage[UniformFanInShape[String, String]] {
    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = ???

    override def shape: UniformFanInShape[String, String] = ???
  }

  object UnsupportedFanOut extends GraphStage[UniformFanOutShape[String, String]] {
    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = ???

    override def shape: UniformFanOutShape[String, String] = ???
  }
}
