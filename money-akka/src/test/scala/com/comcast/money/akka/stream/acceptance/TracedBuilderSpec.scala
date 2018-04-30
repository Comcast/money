package com.comcast.money.akka.stream.acceptance

import akka.stream.{Attributes, UniformFanInShape, UniformFanOutShape}
import akka.stream.scaladsl.{Balance, Broadcast, Interleave, Merge, MergePrioritized}
import akka.stream.stage.{GraphStage, GraphStageLogic}
import com.comcast.money.akka.Blocking.RichFuture
import com.comcast.money.akka.stream.{UnsupportedUniformFanInShape, UnsupportedUniformFanOutShape}
import com.comcast.money.akka.{AkkaMoneyScope, TestStreams}

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
      val expectedSpanNames = replicateAndAppend(Seq(stream, fanInOfString, stringToString, stringToString, fanOutOfString))

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
