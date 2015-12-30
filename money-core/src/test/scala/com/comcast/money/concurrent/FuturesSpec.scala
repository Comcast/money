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

package com.comcast.money.concurrent

import com.comcast.money.api.SpanId
import com.comcast.money.core.Money.tracer
import com.comcast.money.core.Tracers
import com.comcast.money.emitters.LogRecord
import com.comcast.money.internal.SpanLocal
import com.comcast.money.test.AkkaTestJawn
import com.comcast.money.util.DateTimeUtil
import org.scalatest._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class FuturesSpec extends AkkaTestJawn
    with FeatureSpecLike
    with GivenWhenThen
    with Matchers
    with BeforeAndAfterEach
    with OneInstancePerTest {

  import Futures._

  override def beforeEach(): Unit = {
    LogRecord.clear()
    SpanLocal.clear()
    DateTimeUtil.timeProvider = DateTimeUtil.SystemMicroTimeProvider
  }

  feature("Traced futures") {
    scenario("Nested traces that propagate futures") {
      Given("a traced future that has a nested traced future in it")
      And("each future uses flatMap and map")
      val fut = newTrace("root") {
        tracer.record("begin", "root")
        println(s"\r\n; begin root ${SpanLocal.current}")

        newTrace("nested") {
          tracer.record("begin", "nested")
          println(s"\r\n; begin nested ${SpanLocal.current}")
          Some(456)
        }.flatMap {
          case Some(v) =>
            tracer.record("flatMap", "nested")
            println(s"\r\n; flatMap nested ${SpanLocal.current}")
            Future {
              v
            }
        }.map {
          case _ =>
            tracer.record("map", "nested")
            println(s"\r\n; map nested ${SpanLocal.current}")
            SpanLocal.current.get
        }
      }.flatMap { child =>
        tracer.record("flatMap", "root")
        println(s"\r\n; flatMap root ${SpanLocal.current}")
        child
      }.map {
        case s =>
          tracer.record("map", "root")
          println(s"\r\n; map root ${SpanLocal.current}")
          (SpanLocal.current.get, s)
      }

      When("the future completes execution")
      val (rootSpanId, nestedSpanId) = Await.result(fut, 2 seconds)

      Then("the parent of the nested trace is the root trace")
      nestedSpanId.parentId shouldEqual rootSpanId.selfId

      And("the root trace has notes for begin, flatMap, and map")
      expectLogMessageContaining("[ begin=root ][ flatMap=root ][ map=root ]")

      And("the nested trace has notes for begin, flatMap, and map")
      expectLogMessageContaining("[ begin=nested ][ flatMap=nested ][ map=nested ]")
    }

    scenario("foreach") {
      Given("a traced future that returns a string value")
      val fut = newTrace("root") {
        println("beginning root")
        tracer.record("begin", "root")
        "100"
      }

      And("a foreach exists on the traced future that records a note and converts the string to a double")
      fut.foreach { v =>
        println("foreach")
        tracer.record("gater", v.toDouble)
        v.toDouble
      }

      When("the future is executed")
      Await.result(fut, 2 seconds)

      Then("the trace contains notes from both the main future method and the foreach method")
      expectLogMessageContaining("[ begin=root ][ gater=100.0 ]")
    }

    scenario("filter") {
      Given("a traced future that runs a filter")
      val fut = newTrace("root") {
        tracer.record("begin", "root")
        println(s"\r\n; begin root ${SpanLocal.current}")
        100
      }.map { v =>
        tracer.record("map", "root")
        println(s"\r\n; map root ${SpanLocal.current}")
        v
      }.filter(_ <= 100).map { v =>
        tracer.record("map2", "root")
        println(s"\r\n; map2 root ${SpanLocal.current}")
        200
      }

      When("the future is executed")
      Await.result(fut, 2 seconds)

      Then(
        "the trace contains the items recorded from the main future, map, and the note recorded after the filter is " +
          "applied"
      )
      expectLogMessageContaining("[ begin=root ][ map=root ][ map2=root ]")
    }

    scenario("filter does not match") {
      Given("a traced future that runs a filter whose filter does not match")
      val fut = newTrace("root") {
        tracer.record("begin", "root")
        println(s"\r\n; begin root ${SpanLocal.current}")
        100
      }.map { v =>
        tracer.record("map", "root")
        println(s"\r\n; map root ${SpanLocal.current}")
        v
      }.filter(_ > 1000).map { v =>
        tracer.record("map2", "root")
        println(s"\r\n; map2 root ${SpanLocal.current}")
        200
      }

      When("the future completes")

      Then("a NoSuchElementException is thrown")
      intercept[NoSuchElementException] {
        Await.result(fut, 2 seconds)
      }

      And("the trace span does not contain the notes recorded after the filter")
      And("the result of the trace span is a failure")
      expectLogMessageContainingStrings(Seq("begin=root", "map=root", "span-success=false"))
    }

    scenario("collect") {
      Given("a traced future that runs a collect and the collect matches")
      val fut = newTrace("root") {
        tracer.record("begin", "root")
        println(s"\r\n; begin root ${SpanLocal.current}")
        100
      }.map { v =>
        tracer.record("map", "root")
        println(s"\r\n; map root ${SpanLocal.current}")
        v
      }.collect {
        case v =>
          tracer.record("collect", "root")
          v
      }.map { v =>
        tracer.record("map2", "root")
        println(s"\r\n; map2 root ${SpanLocal.current}")
        200
      }

      When("the future completes")
      Await.result(fut, 2 seconds)

      Then("the trace span contains notes recorded in all functions attached to the future")
      expectLogMessageContainingStrings(Seq("begin=root", "collect=root", "map=root", "map2=root", "span-success=true"))
    }

    scenario("collect does not match") {
      Given("a traced future that has a collect that does not match")
      val fut = newTrace("root") {
        tracer.record("begin", "root")
        println(s"\r\n; begin root ${SpanLocal.current}")
        Thread.sleep(500)
        100
      }.map { v =>
        tracer.record("map", "root")
        println(s"\r\n; map root ${SpanLocal.current}")
        v
      }.collect {
        case v if v > 1000 =>
          tracer.record("collect", "root")
          v
      }.map { v =>
        tracer.record("map2", "root")
        println(s"\r\n; map2 root ${SpanLocal.current}")
        200
      }

      When("the future completes")
      Then("a NoSuchElementException is expected")
      intercept[NoSuchElementException] {
        Await.result(fut, 2 seconds)
      }

      And("the trace span contains all notes that were recorded before the collect")
      And("does not contain notes recorded in the collect or after")
      And("the result of the trace span is failure")
      expectLogMessageContainingStrings(Seq("begin=root", "map=root", "span-success=false"))
    }

    scenario("recover") {
      Given("a traced future that throws an expected exception has a recover attached")
      val fut = newTrace("root") {
        tracer.record("begin", "root")
        println(s"\r\n; begin root ${SpanLocal.current}")
        100
      }.map { v =>
        tracer.record("map", "root")
        println(s"\r\n; map root ${SpanLocal.current}")
        throw new IllegalArgumentException("fail")
      }.recover {
        case _ =>
          tracer.record("recover", "root")
          0
      }

      When("the future completes")
      Await.result(fut, 2 seconds)

      Then("no exception is thrown")
      And("the trace span contains notes from all of the functions, including the recover")
      And("the result of the trace span is success")
      expectLogMessageContainingStrings(Seq("begin=root", "map=root", "recover=root", "span-success=true"))
    }

    scenario("recover that never is called") {
      Given("a traced future that executes normally")
      And("the traced future has a recover")
      val fut = newTrace("root") {
        tracer.record("begin", "root")
        println(s"\r\n; begin root ${SpanLocal.current}")
        100
      }.map { v =>
        tracer.record("map", "root")
        println(s"\r\n; map root ${SpanLocal.current}")
      }.recover {
        case _ =>
          tracer.record("recover", "root")
          0
      }

      When("the future completes")
      Await.result(fut, 2 seconds)

      Then("the trace span does not record any notes captured inside of the recover")
      expectLogMessageContainingStrings(Seq("begin=root", "map=root", "span-success=true"))
    }

    scenario("nested recover") {
      Given("a traced future that contains a nested trace future")
      And("the nested trace future throws an exception")
      And("the nested trace future has a recover")
      val fut = newTrace("root") {
        tracer.record("begin", "root")
        println(s"\r\n; ${System.nanoTime()} begin root ${SpanLocal.current}")
        Thread.sleep(10)
        100
      }.map { v =>
        tracer.record("map", "root")
        println(s"\r\n; ${Thread.currentThread().getId()} - ${System.nanoTime()} map root ${SpanLocal.current}")
        newTrace("nested") {
          tracer.record("begin", "nested")
          println(s"\r\n; ${Thread.currentThread().getId()} - ${System.nanoTime()} begin nested ${SpanLocal.current}")
          300
        }.map { v =>
          println(
            s"\r\n; ${v.getClass().getName()}; ${Thread.currentThread().getId()} - ${
              System.nanoTime()
            } map nested ${SpanLocal.current}"
          )
          tracer.record("map", "nested")
          throw new IllegalArgumentException("fail")
        }.recover {
          case _ =>
            tracer.record("recover", "nested")
            println(
              s"\r\n; ${v.getClass().getName()}; ${Thread.currentThread().getId()} - ${
                System.nanoTime()
              } map nested ${SpanLocal.current}"
            )
            200
        }
      }.map {
        case _ =>
          0
      }

      When("the traced future completes")
      Await.result(fut, 2 seconds)

      Then("the trace span contains all notes captured by the root future")
      And("the root trace span completes with success")
      expectLogMessageContainingStrings(Seq("begin=root", "map=root", "span-success=true"))

      And("the nested trace span contains all notes captured by the root future")
      And("the nested trace span completes with success")
      expectLogMessageContainingStrings(Seq("begin=nested", "map=nested", "recover=nested", "span-success=true"))
    }

    scenario("failure results in a failed result in the span") {
      Given("a traced future throws an exception")
      val fut = newTrace("root") {
        tracer.record("begin", "root")
        println(s"\r\n; begin root ${SpanLocal.current}")
        Thread.sleep(10)
        100
      }.map { v =>
        tracer.record("map", "root")
        println(s"\r\n; map root ${SpanLocal.current}")
        throw new IllegalArgumentException("fail")
      }

      When("the future completes")
      Then("the exception is thrown")
      intercept[IllegalArgumentException] {
        Await.result(fut, 2 seconds)
      }

      And("the trace span contains all notes recorded")
      And("the trace span completes with a failure")
      expectLogMessageContainingStrings(Seq("begin=root", "map=root", "span-success=false"))
    }

    scenario("transformed on failure") {
      Given("a traced future that contains a transform")
      And("the transform records a note on failure")
      And("the future throws an exception")

      val fut = newTrace("root") {
        tracer.record("begin", "root")
        println(s"\r\n; begin root ${SpanLocal.current}")
        Thread.sleep(10)
        100
      }.map { v =>
        tracer.record("map", "root")
        println(s"\r\n; map root ${SpanLocal.current}")
        throw new IllegalArgumentException("fail")
      }.transform(
        { s =>
          tracer.record("success", "transform")
          200
        }, { t: Throwable =>
          tracer.record("failed", "transform")
          new IllegalStateException("fail on transform")
        }
      )

      When("the future completes")
      Then("the exception is thrown")
      intercept[IllegalStateException] {
        Await.result(fut, 2 seconds)
      }

      And("the trace span contains a note recorded in the failure function on the transform")
      And("the trace span results in a failure")
      expectLogMessageContainingStrings(Seq("begin=root", "failed=transform", "map=root", "span-success=false"))
    }

    scenario("transformed on success") {
      Given("a traced future that has a transform")
      And("the transform records a note on success")
      And("the future runs normally")
      val fut = newTrace("root") {
        tracer.record("begin", "root")
        println(s"\r\n; begin root ${SpanLocal.current}")
        Thread.sleep(10)
        100
      }.map { v =>
        tracer.record("map", "root")
        println(s"\r\n; map root ${SpanLocal.current}")
      }.transform(
        { s =>
          tracer.record("success", "transform")
          200
        }, { t: Throwable =>
          tracer.record("failed", "transform")
          new IllegalStateException("fail on transform")
        }
      )

      When("the future completes")
      Await.result(fut, 2 seconds)

      Then("all notes are captured including the note recorded on success in the transform")
      expectLogMessageContainingStrings(Seq("begin=root", "map=root", "success=transform"))
    }

    scenario("onFailure") {
      Given("a traced future that has an onFailure")
      And("the traced future throws an exception")

      val fut = newTrace("root") {
        tracer.record("begin", "root")
        println(s"\r\n; onFailure begin root ${SpanLocal.current}")
        // Adding a sleep to ensure that we do not complete the future before adding the onFailure
        // if a future is complete already, onFailure will never fire
        Thread.sleep(500)
        100
      }.map { v =>
        tracer.record("map", "root")
        println(s"\r\n; onFailure map root ${SpanLocal.current}")
        try {
          600
        } finally {
          throw new IllegalArgumentException("fail")
        }
      }
      fut.onFailure {
        case _ =>
          println(s"\r\n; onFailure root ${SpanLocal.current}")
          tracer.record("onFailure", "root")
          200
      }

      When("the future completes")
      Then("the exception is thrown")
      And("the result of the onFailure is returned")
      intercept[IllegalArgumentException] {
        val result = Await.result(fut, 2 seconds)
        result shouldEqual 200
      }

      And("the trace span contains all notes recorded, including the note recorded onFailure")
      And("the result of the trace is failure")
      expectLogMessageContainingStrings(Seq("begin=root", "map=root", "onFailure=root", "span-success=false"))
    }

    scenario("onSuccess") {
      Given("a traced future that has an onSuccess")
      val fut = newTrace("root") {
        tracer.record("begin", "root")
        println(s"\r\n; onSuccess begin root ${SpanLocal.current}")
        // Adding a sleep to ensure that we do not complete the future before adding the onSuccess
        // if a future is complete already, onFailure will never fire
        Thread.sleep(500)
        100
      }.map { v =>
        tracer.record("map", "root")
        println(s"\r\n; onSuccess map root ${SpanLocal.current}")
        600
      }

      And("the onSuccess records a note")
      fut.onSuccess {
        case _ =>
          println(s"\r\n; onSuccess root ${SpanLocal.current}")
          tracer.record("onSuccess", "root")
      }

      When("the future completes")
      val result = Await.result(fut, 2 seconds)

      Then("the result of the main future is returned")
      result shouldEqual 600

      And("the trace span contains all notes, including the note recorded onSuccess")
      expectLogMessageContainingStrings(Seq("begin=root", "map=root", "onSuccess=root", "span-success=true"))
    }

    scenario("recoverWith") {
      Given("a traced future that has a recoverWith")
      And("the future throws an exception")
      And("the recover with records a note")
      val fut = newTrace("root") {
        tracer.record("begin", "root")
        println(s"\r\n; begin root ${SpanLocal.current}")
        100
      }.map { v =>
        tracer.record("map", "root")
        println(s"\r\n; map root ${SpanLocal.current}")
        try {
          600
        } finally {
          throw new IllegalArgumentException("fail")
        }
      }.recoverWith {
        case e: IllegalArgumentException =>
          tracer.record("recoverWith", "root")
          Future {
            0
          }
      }

      When("the future completes")
      Await.result(fut, 2 seconds)

      Then("the trace span contains all notes, including the note recorded in recoverWith")
      And("the result of the trace span is success")
      expectLogMessageContainingStrings(Seq("begin=root", "map=root", "recoverWith=root", "span-success=true"))
    }
    scenario("simple for comprehension") {
      Given("two traced futures")
      val fut1 = newTrace("one") {
        tracer.record("in", "one")
        Thread.sleep(50)
        SpanLocal.current.get
      }

      val fut2 = newTrace("two") {
        tracer.record("in", "two")
        Thread.sleep(50)
        SpanLocal.current.get
      }

      And("a for comprehension that operates on each of the futures")
      val simpleMath = for {
        one <- fut1
        two <- fut2
      } yield (one, two)

      When("the for comprehension completes")
      val result = Await.result(simpleMath, 2 seconds)

      Then("the two futures are not linked through a parent id")
      result._2.parentId shouldNot be(result._1.selfId)

      And("the first span contains the note it recorded")
      expectLogMessageContaining("[ in=one ]")

      And("the second span contains the note it recorded")
      expectLogMessageContaining("[ in=two ]")
    }

    scenario("zip") {
      Given("two separately traced futures")
      val fut1 = newTrace("one") {
        tracer.record("in", "one")
        Thread.sleep(50)
        SpanLocal.current.get
      }

      val fut2 = newTrace("two") {
        tracer.record("in", "two")
        Thread.sleep(50)
        SpanLocal.current.get
      }

      And("the futures are zipped")
      val combined = fut1 zip fut2

      When("the zipped future completes")
      val (firstSpanId, secondSpanId) = Await.result(combined, 2 seconds)

      Then("the two futures are not lined through a parent")
      secondSpanId.parentId shouldNot be(firstSpanId.selfId)

      And("the first span contains the message it recorded")
      expectLogMessageContaining("[ in=one ]")

      And("the second span contains the message it recorded")
      expectLogMessageContaining("[ in=two ]")
    }

    scenario("fallbackTo on failure") {
      Given("a traced future that throws an exception")
      val fut = newTrace("one") {
        tracer.record("in", "one")
        Thread.sleep(50)
        try {
          100
        } finally {
          throw new IllegalStateException("fail me")
        }
      }

      And("another traced future that runs normally")
      val fut2 = newTrace("fall") {
        println(s"\r\nfall back guy")
        tracer.record("fall", "guy")
        SpanLocal.current.get
      }

      And("the failing future falls back to the successful future")
      val fut3 = fut fallbackTo fut2

      When("the fallback completes")
      Await.result(fut3, 2 seconds)

      Then("the result of the failing future contains notes it recorded")
      And("completes with a result of failed")
      expectLogMessageContainingStrings(Seq("in=one", "span-success=false"))

      And("the result of the fallback future contains notes it recorded")
      And("completes with a result of success")
      expectLogMessageContainingStrings(Seq("fall=guy", "span-success=true"))
    }

    scenario("fallbackTo on success") {
      Given("a traced future that runs normally")
      val fut = newTrace("one") {
        tracer.record("in", "one")
        Thread.sleep(50)
        100
      }

      And("a fallback traced future that also runs normally")
      val fut2 = newTrace("fall") {
        println(s"\r\nfall back guy")
        tracer.record("fall", "guy")
        SpanLocal.current.get
      }

      And("the first future falls back to the second traced future")
      val fut3 = fut fallbackTo fut2

      When("the fallback completes")
      Await.result(fut3, 2 seconds)

      // Here, both trace will run to completion successfully, just due to the nature of futures
      Then("the first trace span will contain notes it recorded")
      And("the first trace span will result in success")
      expectLogMessageContainingStrings(Seq("in=one", "span-success=true"))

      And("the second trace span will contain notes it recorded")
      And("the second trace span will result in success")
      expectLogMessageContainingStrings(Seq("fall=guy", "span-success=true"))
    }

    scenario("andThen") {
      Given("a traced future followed by an andThen")
      val fut = newTrace("root") {
        tracer.record("in", "root")
        Thread.sleep(50)
        100
      }.andThen {
        case _ =>
          tracer.record("andThen", "here")
          200
      }

      When("the future completes")
      val r = Await.result(fut, 2 seconds)

      Then("the value from the original future is returned as per the docs")
      r shouldBe 100

      And("the trace span contains notes from the main future")
      And("contains notes from the andThen")
      expectLogMessageContainingStrings(Seq("in=root", "andThen=here"))
    }

    scenario("getting a value from a completed future") {
      Given("a future that returns a value")
      val fut = newTrace("one") {
        tracer.record("in", "one")
        Thread.sleep(50)
        100
      }

      When("the future completes")
      Await.result(fut, 2 seconds)

      And("we retrieve the value from the future")
      val r = fut.value

      Then("the value that comes from the future is returned")
      r.get.get shouldBe 100

      And("the future is complete")
      fut.isCompleted shouldBe true
    }

    scenario("failed") {
      Given("a future that throws an exception")
      val fut = newTrace("one") {
        tracer.record("in", "one")
        Thread.sleep(50)
        try {
          100
        } finally {
          throw new IllegalArgumentException("failed")
        }
      }

      And("the failed method is attached to the future")
      val futFailed = fut.failed

      When("the failed future completes")
      val result = Await.result(futFailed, 2 seconds)

      Then("the result of the failed future is the exception that was thrown")
      result shouldBe an[IllegalArgumentException]

      And("the trace span records the notes from the original future")
      And("complete with a result of success because the future was expected to fail")
      expectLogMessageContainingStrings(Seq("in=one", "span-success=true"))
    }

    scenario("failed that doesn't fire") {
      Given("a future that runs normally")
      val fut = newTrace("one") {
        tracer.record("in", "one")
        Thread.sleep(50)
        100
      }
      Await.result(fut, 2 seconds)

      And("the failed method is attached to the future")
      val futFailed = fut.failed

      When("the failed future completes")
      intercept[NoSuchElementException] {
        Await.result(futFailed, 2 seconds)
      }

      Then("the trace span records the notes from the original future")
      And("complete with a result of failure because the future was expected to fail")
      expectLogMessageContainingStrings(Seq("in=one", "span-success=false"))
    }
  }

  feature("sub tracing from a traced future") {
    scenario("continuing a trace to a disconnected traced method") {
      Given("a traced future that calls out to a normal method")
      And("the normal method is also traced")
      val fut = newTrace("root") {
        tracer.record("begin", "root")
        println(s"\r\n; begin root ${SpanLocal.current}")
        100
      }.map { v =>
        tracer.record("map", "root")
        val continuedSpanId = continued("root")
        println(s"\r\n; map root ${SpanLocal.current}")
        (SpanLocal.current.get, continuedSpanId.get)
      }

      When("the future completes")
      val (rootSpanId, continuedSpanId) = Await.result(fut, 2 seconds)

      Then("the parent id of the traced method is the span id of the root traced future")
      continuedSpanId.parentId shouldEqual rootSpanId.selfId

      And("the trace span for the future has all notes it recorded")
      expectLogMessageContaining("[ begin=root ][ map=root ]")

      And("the trace span for the method call has all notes it recorded")
      expectLogMessageContaining("[ continue=root ]")
    }
  }

  feature("continuing a trace in a different future from a traced future") {
    scenario("same execution context") {
      Given("a traced future that contains a future that it wants included in its trace")
      val fut = newTrace("root") {
        tracer.record("begin", "root")
        continueTrace {
          tracer.record("within", "root")
          300
        }.map(_ * 2)
      }.map { v =>
        tracer.record("map", "root")
        200
      }

      When("the future completes")
      Await.result(fut, 2 seconds)

      Then("the trace span contains notes recorded from the traced future")
      And("the trace span contains notes recorded from the included traced future")
      expectLogMessageContainingStrings(Seq("begin=root", "map=root", "within=root"))
    }

    scenario("no trace span present") {
      Given("an attempt to use inTracedFuture when no trace future is present")
      val fut = continueTrace {
        300
      }

      Then("the future is not wrapped in a traced future")
      fut should not be a[TracedFuture[_]]
    }
  }

  feature("wrapping a future in a trace") {
    scenario("trace span is present") {
      Given("a traced future that wraps a function block that returns a future")
      val fut = newTrace("wrap") {
        Thread.sleep(1)
        tracer.record("root", "one")
        SpanLocal.current
      }.flatMap { rootSpanId =>
        wrapTrace {
          wrapMe()
        } map { _ =>
          tracer.record("wrapped", "two")
          (rootSpanId, SpanLocal.current)
        }
      }

      When("The future is executed")
      val (rootSpanId, wrappedSpanId) = Await.result(fut, 2 seconds)

      Then("the span inside of the wrapped trace is the same as the span of the root traced future")
      rootSpanId shouldEqual wrappedSpanId

      And("the tracing that took place in the wrapped trace is logged")
      expectLogMessageContainingStrings(Seq("root=one", "wrapped=two"))
    }

    scenario("no trace span is present") {
      Given("an attempt to wrap a future that is not in a trace")
      val fut = wrapTrace {
        wrapMe()
      } map { wm =>
        tracer.record("wrapped", "two")
        SpanLocal.current
      }

      When("The future is executed")
      val result = Await.result(fut, 2 seconds)

      Then("there was no span inside the wrapped trace")
      result shouldBe None
    }
  }

  def continued(from: String): Option[SpanId] = Tracers.traced("continue") {
    tracer.record("continue", from)
    SpanLocal.current
  }

  def wrapMe(): Future[Int] = Future {
    100
  }
}
