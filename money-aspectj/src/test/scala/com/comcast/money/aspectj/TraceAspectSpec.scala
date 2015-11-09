package com.comcast.money.aspectj

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.comcast.money.annotations.{TracedData, Timed, Traced}
import com.comcast.money.core.Money
import com.comcast.money.emitters.LogRecord
import com.comcast.money.internal.MDCSupport
import org.aspectj.lang.ProceedingJoinPoint
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.mock.MockitoSugar

import scala.concurrent.duration._


class TraceAspectSpec extends TestKit(ActorSystem("money", Money.config.getConfig("money.akka")))
with WordSpecLike with GivenWhenThen with OneInstancePerTest with BeforeAndAfterEach with Matchers with MockitoSugar {

  @Traced("methodWithArguments")
  def methodWithArguments(@TracedData("foo") foo: String, @TracedData("CUSTOM_NAME") bar: String) = {
    Thread.sleep(50)
  }

  @Traced("methodWithoutArguments")
  def methodWithoutArguments() = {
    Thread.sleep(50)
  }

  @Traced("methodThrowingException")
  def methodThrowingException() = {
    Thread.sleep(50)
    throw new RuntimeException("test failure")
  }

  @Traced("methodThrowingExceptionWithNoMessage")
  def methodThrowingExceptionWithNoMessage() = {
    Thread.sleep(50)
    throw new RuntimeException()
  }

  @Traced("methodWithArgumentsPropagated")
  def methodWithArgumentsPropagated(@TracedData(value="PROPAGATE", propagate=true) foo: String, @TracedData("CUSTOM_NAME") bar: String) = {
    Thread.sleep(50)
    methodWithoutArguments()
  }

  @Timed("methodWithTiming")
  def methodWithTiming() = {
    Thread.sleep(50)
  }

  def expectLogMessageContaining(contains: String, wait: FiniteDuration = 2.seconds) {
    awaitCond(LogRecord.contains("log")(_.contains(contains)), wait, 100 milliseconds, s"Expected log message containing string $contains not found after $wait")
  }

  def expectLogMessageContainingStrings(strings: Seq[String], wait: FiniteDuration = 2.seconds) {
    awaitCond(LogRecord.contains("log")(s => strings.forall(s.contains)), wait, 100 milliseconds, s"Expected log message containing $strings not found after $wait")
  }

  val mockMdcSupport = mock[MDCSupport]
  object testTraceAspect extends TraceAspect {
    override val mdcSupport:MDCSupport = mockMdcSupport
  }

  override def beforeEach() = {
    reset(mockMdcSupport)
  }

  "TraceAspect" when {
    "advising methods by tracing them" should {
      "handle methods that have no arguments" in {
        Given("a method that has the tracing annotation but has no arguments")
        When("the method is invoked")
        methodWithoutArguments()

        Then("the method execution is traced")
        expectLogMessageContaining("methodWithoutArguments")

        And("the result of success is captured")
        expectLogMessageContaining("span-success=true")
      }
      "complete the trace for methods that throw exceptions" in {
        Given("a method that throws an exception")

        When("the method is invoked")
        a[RuntimeException] should be thrownBy {
          methodThrowingException()
        }

        Then("the method execution is logged")
        expectLogMessageContaining("methodThrowingException")

        And("a span-success is logged with a value of false")
        expectLogMessageContaining("span-success=false")
      }
    }
    "advising methods that have parameters with the TracedData annotation" should {
      "record the value of the parameter in the trace" in {
        Given("a method that has arguments with the TraceData annotation")

        When("the method is invoked")
        methodWithArguments("hello", "bob")

        Then("The method execution is logged")
        expectLogMessageContaining("methodWithArguments")

        And("the values of the arguments that have the TracedData annotation are logged")
        expectLogMessageContaining("hello")

        And("the values of the arguments that have a custom name for the TracedData annotation log using the custom name")
        expectLogMessageContaining("CUSTOM_NAME=bob")
      }
      "record parameters whose value is null" in {
        Given("a method that has arguments with the TraceData annotation")

        When("the method is invoked with a null value")
        methodWithArguments(null, null)

        Then("The method execution is logged")
        expectLogMessageContaining("methodWithArguments")

        And("the parameter values are captured")
        expectLogMessageContaining("foo=")
        expectLogMessageContaining("CUSTOM_NAME=")
      }
      "propagate traced data parameters" in {
        Given("a method that has arguments with the TracedData annotation")
        And("one of those arguments is set to propagate")
        And("the method calls another method that is also traced")

        When("the method is invoked")
        methodWithArgumentsPropagated("boo", "far")

        Then("the main method execution is logged")
        expectLogMessageContainingStrings(Seq("methodWithArgumentsPropagated", "PROPAGATE=boo", "CUSTOM_NAME=far"))

        And("the child span has the propagated parameters")
        expectLogMessageContainingStrings(Seq("methodWithoutArguments", "PROPAGATE=boo"))
      }
    }
    "timing method execution" should {
      "record the execution time of a method that returns normally" in {
        Given("a trace exists")
        Money.tracer.startSpan("test-timing")
        And("a method that has the Timed annotation")

        When("the method is called")
        methodWithTiming()

        And("the trace is stopped")
        Money.tracer.stopSpan()

        Then("a message is logged containing the duration of the method execution")
        expectLogMessageContaining("methodWithTiming")
      }
    }
    "testing pointcuts" should {
      "love us some code coverage" in {
        val traceAspect = new TraceAspect()
        traceAspect.traced(null)
        traceAspect.timed(null)
      }
    }
    "advising methods" should {
      "set span name in MDC" in {
        val jp = mock[ProceedingJoinPoint]
        val ann = mock[Traced]
        doReturn("testSpanName").when(ann).value()
        doReturn(None).when(mockMdcSupport).getSpanNameMDC
        testTraceAspect.adviseMethodsWithTracing(jp, ann)

        verify(mockMdcSupport).setSpanNameMDC(Some("testSpanName"))
        verify(mockMdcSupport).setSpanNameMDC(None)
      }
      "save the current span name and reset after the child span is complete" in {
        val jp = mock[ProceedingJoinPoint]
        val ann = mock[Traced]
        doReturn("testSpanName").when(ann).value()
        doReturn(Some("parentSpan")).when(mockMdcSupport).getSpanNameMDC

        testTraceAspect.adviseMethodsWithTracing(jp, ann)

        verify(mockMdcSupport).setSpanNameMDC(Some("parentSpan"))
      }
    }
  }
}
