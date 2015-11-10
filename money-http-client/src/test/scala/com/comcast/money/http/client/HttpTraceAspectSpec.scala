package com.comcast.money.http.client

import java.io.{ByteArrayInputStream, InputStream}

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.comcast.money.annotations.Traced
import com.comcast.money.core._
import com.comcast.money.emitters._
import com.comcast.money.internal.SpanLocal
import org.apache.http._
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.{HttpClient, ResponseHandler}
import org.apache.http.util.EntityUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.mock.MockitoSugar

import scala.concurrent.duration._

class HttpTraceAspectSpec extends TestKit(ActorSystem("money", Money.config.getConfig("money.akka")))
with FeatureSpecLike
with Matchers
with MockitoSugar
with OneInstancePerTest
with GivenWhenThen
with BeforeAndAfter
with BeforeAndAfterAll {

  val mockHttpClient: HttpClient = mock[HttpClient]
  val mockHttpRequest: HttpUriRequest = mock[HttpUriRequest]
  val mockHttpResponse: HttpResponse = mock[HttpResponse]
  val mockStatusLine: StatusLine = mock[StatusLine]
  val mockHttpEntity: HttpEntity = mock[HttpEntity]
  val mockHttpResponseHandler: ResponseHandler[String] = mock[ResponseHandler[String]]
  val responseEntityStream: InputStream = new ByteArrayInputStream("test-response".getBytes)

  // -- SAMPLE METHODS WEAVED BY OUR ASPECT
  @Traced("methodWithHttpCallUsingEntityUtils")
  def methodWithHttpCallUsingEntityUtils(): String = {
    val response = mockHttpClient.execute(mockHttpRequest)
    Thread.sleep(50)
    val entity = EntityUtils.toString(response.getEntity)
    Thread.sleep(10)
    entity
  }

  @Traced("methodWithHttpCallUsingResponseHandler")
  def methodWithHttpCallUsingResponseHandler(): String = {
    val response: String = mockHttpClient.execute(mockHttpRequest, mockHttpResponseHandler)
    Thread.sleep(50)
    response
  }

  def expectLogMessageContaining(contains: String, wait: FiniteDuration = 2.seconds) {
    awaitCond(
      LogRecord.contains("log")(_.contains(contains)), wait, 100 milliseconds,
      s"Expected log message containing string $contains not found after $wait")
  }

  // Used by some tests that cannot be adequately integration tested
  val mockTracer: Tracer = mock[Tracer]
  val mockJoinPoint: ProceedingJoinPoint = mock[ProceedingJoinPoint]
  val mockTracedAnnotation: Traced = mock[Traced]
  val testAspect = new HttpTraceAspect {
    override def tracer: Tracer = mockTracer
  }

  before {
    doReturn(responseEntityStream).when(mockHttpEntity).getContent
    doReturn(200).when(mockStatusLine).getStatusCode
    doReturn(mockStatusLine).when(mockHttpResponse).getStatusLine
    doReturn(mockHttpEntity).when(mockHttpResponse).getEntity
    doReturn(mockHttpResponse).when(mockHttpClient).execute(mockHttpRequest)
    doReturn(mockHttpResponse).when(mockJoinPoint).proceed()
    doReturn("test-response").when(mockHttpResponseHandler).handleResponse(mockHttpResponse)
  }

  after {
    SpanLocal.clear()
  }

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  feature("Capturing http metrics on a method that calls http client execute returning an HttpResponse") {
    scenario("happy path") {
      Given("a method exists with the trace annotation")
      When("the method that calls the http client is invoked")
      val result = methodWithHttpCallUsingEntityUtils()

      Then("the default metrics are recorded in the span")
      expectLogMessageContaining("methodWithHttpCallUsingEntityUtils")
      expectLogMessageContaining("http-call-duration")
      expectLogMessageContaining("http-call-with-body-duration")
      expectLogMessageContaining("http-process-response-duration")

      And("any metrics with names that are overridden through configuration are also recorded in the span")
      // responseCode is an override in the test resources application.conf file
      expectLogMessageContaining("responseCode")

      And("the result from the method is returned")
      result shouldEqual "test-response"
    }
    scenario("http client execute throws an exception") {
      Given("a method exists with the trace annotation")
      And("the http client call throws an exception")
      doThrow(new IllegalStateException()).when(mockHttpClient).execute(mockHttpRequest)

      When("the method is invoked")
      intercept[IllegalStateException] {
        methodWithHttpCallUsingEntityUtils()
      }

      Then("the metrics are still recorded in the span")
      expectLogMessageContaining("methodWithHttpCallUsingEntityUtils")
      expectLogMessageContaining("http-call-duration")
      expectLogMessageContaining("http-call-with-body-duration")
      expectLogMessageContaining("http-process-response-duration")
    }
    scenario("an exception is thrown while consuming the response body") {
      Given("a method exists with the trace annotation")
      And("getting the content from the response throws an exception")
      doThrow(new IllegalStateException()).when(mockHttpEntity).getContent

      When("the method is invoked")
      intercept[IllegalStateException] {
        methodWithHttpCallUsingEntityUtils()
      }

      Then("the metrics are still recorded in the span")
      expectLogMessageContaining("methodWithHttpCallUsingEntityUtils")
      expectLogMessageContaining("http-call-duration")
      expectLogMessageContaining("http-call-with-body-duration")
      expectLogMessageContaining("http-process-response-duration")
    }
  }
  feature("Capturing http metrics on a method that calls http client and passes in a response handler") {
    scenario("happy path") {
      Given("the method calls execute on an Http Client passing in a response handler")
      doReturn("test-response").when(mockHttpClient).execute(mockHttpRequest, mockHttpResponseHandler)

      When("the method is invoked")
      val result = methodWithHttpCallUsingResponseHandler()

      Then("the default metrics are recorded in the span")
      expectLogMessageContaining("methodWithHttpCallUsingEntityUtils")
      expectLogMessageContaining("http-call-duration")
      expectLogMessageContaining("http-call-with-body-duration")
      expectLogMessageContaining("http-process-response-duration")

      And("any metrics with names that are overridden through configuration are also recorded in the span")
      // responseCode is an override in the test resources application.conf file
      expectLogMessageContaining("responseCode")

      And("the result from the method is returned")
      result shouldEqual "test-response"
    }
  }
  feature("advising calls to the http response handler") {
    scenario("happy path") {
      Given("a response handler that returns a simple value is advised")
      doReturn("test-result").when(mockJoinPoint).proceed()

      And("a traced annotation is present")
      doReturn("test-annotation").when(mockTracedAnnotation).value()

      And("a http response has a 200 status code")
      doReturn(200).when(mockStatusLine).getStatusCode
      doReturn(mockStatusLine).when(mockHttpResponse).getStatusLine

      When("the response handler is advised by the Http Trace Aspect")
      val result = testAspect.adviseHttpResponseHandler(mockJoinPoint, mockHttpResponse, mockTracedAnnotation)

      Then("the http call duration is ended")
      verify(mockTracer).stopTimer("http-call-duration")

      And("the status code is recorded")
      verify(mockTracer).record("responseCode", 200)

      And("the joinpoint is executed")
      verify(mockJoinPoint).proceed()

      And("the result of executing the joinpoint is returned")
      result shouldEqual "test-result"
    }
    scenario("the response handler throws an exception") {
      Given("a response handler throws an exception")
      doThrow(new IllegalStateException()).when(mockJoinPoint).proceed()

      And("a traced annotation is present")
      doReturn("test-annotation").when(mockTracedAnnotation).value()

      When("the response handler is advised by the Http Trace Aspect")
      intercept[IllegalStateException] {
        testAspect.adviseHttpResponseHandler(mockJoinPoint, mockHttpResponse, mockTracedAnnotation)
      }

      Then("the http call duration is ended")
      verify(mockTracer).stopTimer("http-call-duration")

      And("the status code is recorded as the value that was returned from the service")
      verify(mockTracer).record("responseCode", 200)
    }
    scenario("getting the response code throws an exception") {
      Given("an http response that throws an exception when retrieving the response code")
      doThrow(new IllegalStateException()).when(mockHttpResponse).getStatusLine

      And("a traced annotation is present")
      doReturn("test-annotation").when(mockTracedAnnotation).value()

      When("the response handler is advised by the Http Trace Aspect")
      intercept[IllegalStateException] {
        testAspect.adviseHttpResponseHandler(mockJoinPoint, mockHttpResponse, mockTracedAnnotation)
      }

      Then("the http call duration is ended")
      verify(mockTracer).stopTimer("http-call-duration")

      And("the status code is recorded as 0")
      verify(mockTracer).record("responseCode", 0)
    }
    scenario("the http response is null") {
      Given("a call to the response handler passes in an http response that is null")

      And("a traced annotation is present")
      doReturn("test-annotation").when(mockTracedAnnotation).value()

      When("the response handler is advised by the Http Trace Aspect")
      testAspect.adviseHttpResponseHandler(mockJoinPoint, null, mockTracedAnnotation)

      Then("the http call duration is ended")
      verify(mockTracer).stopTimer("http-call-duration")

      And("the status code is recorded as 0")
      verify(mockTracer).record("responseCode", 0)
    }
  }
  feature("advising the call to http client execute with a response handler") {
    scenario("happy path") {
      Given("A call to http client that takes an http response handler")
      doReturn("test-annotation").when(mockTracedAnnotation).value()

      And("a span has been started")
      SpanLocal.push(SpanId(1L))

      When("The method the uses the http client is invoked")
      testAspect.adviseHttpClientExecuteToResponseHandler(mockHttpRequest, mockTracedAnnotation)

      Then("the http call timer was started")
      verify(mockTracer).startTimer("http-call-duration")

      And("the http call with body timer was started")
      verify(mockTracer).startTimer("http-call-with-body-duration")

      And("the money trace header is added to the request")
      verify(mockHttpRequest).setHeader("X-MoneyTrace", "trace-id=1;parent-id=1;span-id=1")
    }
    scenario("no span has been started") {
      Given("A call to http client that takes an http response handler")
      doReturn("test-annotation").when(mockTracedAnnotation).value()

      And("a span has NOT been started")
      // no span started here

      When("The method the uses the http client is invoked")
      testAspect.adviseHttpClientExecuteToResponseHandler(mockHttpRequest, mockTracedAnnotation)

      Then("the http call timer was started")
      verify(mockTracer).startTimer("http-call-duration")

      And("the http call with body timer was started")
      verify(mockTracer).startTimer("http-call-with-body-duration")

      And("the money span header is never added")
      verifyZeroInteractions(mockHttpRequest)
    }
    scenario("the http request is null") {
      Given("A call to http client that takes an http response handler")
      doReturn("test-annotation").when(mockTracedAnnotation).value()

      And("a span has been started")
      SpanLocal.push(SpanId(1L))

      When("The method the uses the http client is invoked")
      testAspect.adviseHttpClientExecuteToResponseHandler(null, mockTracedAnnotation)

      Then("the http call timer was started")
      verify(mockTracer).startTimer("http-call-duration")

      And("the http call with body timer was started")
      verify(mockTracer).startTimer("http-call-with-body-duration")

      And("the money span header is never added")
      verifyZeroInteractions(mockHttpRequest)
    }
  }
  feature("advising a method that calls http client execute that returns an HttpResponse") {
    scenario("happy path") {
      Given("A call to http client that returns an http response is being traced")
      doReturn("test-annotation").when(mockTracedAnnotation).value()

      And("the http response code returned is a 204")
      doReturn(204).when(mockStatusLine).getStatusCode

      And("a span has been started")
      SpanLocal.push(SpanId(1L))

      When("The method the uses the http client is invoked")
      val result = testAspect.adviseHttpClientExecute(mockJoinPoint, mockHttpRequest, mockTracedAnnotation)

      Then("the http call timer was started")
      verify(mockTracer).startTimer("http-call-duration")

      And("the http call with body timer was started")
      verify(mockTracer).startTimer("http-call-with-body-duration")

      And("the status code of the response is recorded")
      verify(mockTracer).record("responseCode", 204)

      And("the http call with body timer is stopped")
      verify(mockTracer).stopTimer("http-call-duration")

      And("the http response is returned from the advise")
      result shouldEqual mockHttpResponse

      And("the money trace header is added to the request")
      verify(mockHttpRequest).setHeader("X-MoneyTrace", "trace-id=1;parent-id=1;span-id=1")
    }
  }
  feature("test coverage") {
    scenario("loves us some code coverage") {
      testAspect.httpClientExecute(mockHttpRequest) shouldEqual()
      testAspect.httpClientExecuteToResponseHandler(mockHttpRequest) shouldEqual()
      testAspect.httpResponseHandler(mockHttpResponse) shouldEqual()
      testAspect.consumeHttpEntity() shouldEqual()
      testAspect.traced(mockTracedAnnotation) shouldEqual()
    }
  }
}
