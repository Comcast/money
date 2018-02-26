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

package com.comcast.money.http.client

import java.io.Closeable
import java.util.concurrent.{ CompletableFuture, Executors, Future, TimeUnit }

import com.comcast.money.api.SpanId
import com.comcast.money.core.{ SpecHelpers, Tracer, Formatters => CoreSpanId }
import com.comcast.money.core.internal.SpanLocal
import org.apache.http.client.methods.{ CloseableHttpResponse, HttpUriRequest }
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.nio.client.HttpAsyncClient
import org.apache.http.nio.protocol.{ HttpAsyncRequestProducer, HttpAsyncResponseConsumer }
import org.apache.http.protocol.HttpContext
import org.apache.http.{ HttpHost, HttpResponse, StatusLine }
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.mockito.Matchers.{ eq => argEq }
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest._
import org.scalatest.mock.MockitoSugar

class TraceFriendlyHttpAsyncClientSpec extends WordSpec with SpecHelpers
    with Matchers with MockitoSugar with OneInstancePerTest with BeforeAndAfterEach {

  val httpClient = mock[CloseableHttpAsyncClient]
  val httpUriRequest = mock[HttpUriRequest]
  val httpResponse = mock[CloseableHttpResponse]
  val statusLine = mock[StatusLine]
  val httpHost = new HttpHost("localhost")
  val httpContext = mock[HttpContext]
  val callback = mock[FutureCallback[HttpResponse]]
  val requestProducer = mock[HttpAsyncRequestProducer]
  val responseConsumer = mock[HttpAsyncResponseConsumer[HttpResponse]]
  val spanId = new SpanId()
  val future = new CompletableFuture[HttpResponse]()

  val callbackCaptor = ArgumentCaptor.forClass(classOf[FutureCallback[HttpResponse]])
  val producerCaptor = ArgumentCaptor.forClass(classOf[HttpAsyncRequestProducer])
  val consumerCaptor = ArgumentCaptor.forClass(classOf[HttpAsyncResponseConsumer[HttpResponse]])

  val executor = Executors.newScheduledThreadPool(1)

  var callbackSpanId: Option[SpanId] = None

  when(httpResponse.getStatusLine).thenReturn(statusLine)
  when(statusLine.getStatusCode).thenReturn(200)

  // extend what we are testing so we can use a mock tracer
  val underTest = new TraceFriendlyHttpAsyncClient(httpClient) {
    override val tracer = mock[Tracer]
  }

  override def beforeEach(): Unit = {
    SpanLocal.push(testSpan(spanId))
    doReturn(httpUriRequest).when(requestProducer).generateRequest()
    doReturn(httpResponse).when(responseConsumer).getResult

    doAnswer(new Answer[Void] {
      override def answer(invocation: InvocationOnMock): Void = {
        // copy span ID of current thread to field to match in test
        callbackSpanId = SpanLocal.current.map(_.info.id)
        null
      }
    }).when(callback).completed(httpResponse)
  }

  // if you don't reset, then the verifies are going to be off
  override def afterEach() = {
    reset(underTest.tracer, httpUriRequest, httpClient)
    SpanLocal.clear()
  }

  def verifyTracing() = {
    verify(underTest.tracer).startTimer(HttpAsyncTraceConfig.HttpResponseTimeTraceKey)
    verify(underTest.tracer).stopTimer(HttpAsyncTraceConfig.HttpResponseTimeTraceKey)
    verify(underTest.tracer).record("http-response-code", 200L)
    verify(httpUriRequest).setHeader("X-MoneyTrace", CoreSpanId.toHttpHeader(spanId))
    assert(callbackSpanId.contains(spanId))
  }

  def completeCallback(): Unit = {
    val callback = callbackCaptor.getValue
    val runnable = new Runnable {
      override def run(): Unit = {
        callback.completed(httpResponse)
        future.complete(httpResponse)
      }
    }
    executor.schedule(runnable, 50, TimeUnit.MILLISECONDS)
    future.get()
  }

  "TraceFriendlyHttpAsyncClient" should {
    "record the status code and call duration when execute(HttpUriRequest, FutureCallback)" in {
      underTest.execute(httpUriRequest, callback)

      verify(httpClient).execute(argEq(httpUriRequest), callbackCaptor.capture())
      completeCallback()

      verify(callback).completed(httpResponse)
      verifyTracing()
    }
    "record the status code and call duration when execute(HttpUriRequest, HttpContext, FutureCallback)" in {
      underTest.execute(httpUriRequest, httpContext, callback)

      verify(httpClient).execute(argEq(httpUriRequest), argEq(httpContext), callbackCaptor.capture())
      completeCallback()

      verify(callback).completed(httpResponse)
      verifyTracing()
    }
    "record the status code and call duration when execute(HttpHost, HttpRequest, FutureCallback)" in {
      underTest.execute(httpHost, httpUriRequest, callback)

      verify(httpClient).execute(argEq(httpHost), argEq(httpUriRequest), callbackCaptor.capture())
      completeCallback()

      verify(callback).completed(httpResponse)
      verifyTracing()
    }
    "record the status code and call duration when execute(HttpHost, HttpRequest, HttpContext, FutureCallback)" in {
      underTest.execute(httpHost, httpUriRequest, httpContext, callback)

      verify(httpClient).execute(argEq(httpHost), argEq(httpUriRequest), argEq(httpContext), callbackCaptor.capture())
      completeCallback()

      verify(callback).completed(httpResponse)
      verifyTracing()
    }
    "record the status code and call duration when execute(HttpAsyncRequestProducer, HttpAsyncResponseConsumer, FutureCallback)" in {
      underTest.execute(requestProducer, responseConsumer, callback)

      verify(httpClient).execute(producerCaptor.capture(), consumerCaptor.capture(), callbackCaptor.capture())
      producerCaptor.getValue.generateRequest()
      val captured = consumerCaptor.getValue
      captured.responseReceived(httpResponse)
      captured.responseCompleted(httpContext)
      completeCallback()

      verify(callback).completed(httpResponse)
      verifyTracing()
    }
    "record the status code and call duration when execute(HttpAsyncRequestProducer, HttpAsyncResponseConsumer, HttpContext, FutureCallback)" in {
      underTest.execute(requestProducer, responseConsumer, httpContext, callback)

      verify(httpClient).execute(producerCaptor.capture(), consumerCaptor.capture(), argEq(httpContext), callbackCaptor.capture())
      producerCaptor.getValue.generateRequest()
      val capturedConsumer = consumerCaptor.getValue
      capturedConsumer.responseReceived(httpResponse)
      capturedConsumer.responseCompleted(httpContext)
      completeCallback()

      verify(callback).completed(httpResponse)
      verifyTracing()
    }
    "records a zero for a status code on exception" in {
      underTest.execute(httpUriRequest, callback)

      verify(httpClient).execute(argEq(httpUriRequest), callbackCaptor.capture())
      callbackCaptor.getValue.failed(new RuntimeException("bad"))

      verify(underTest.tracer).record("http-response-code", 0L)
    }
    "calls close on closeable http client" in {
      underTest.close()
      verify(httpClient).close()
    }
    "calls close if the http client implements closable" in {
      trait Closer extends HttpAsyncClient with Closeable

      val closeHttp = mock[Closer]
      val closeTest = new TraceFriendlyHttpAsyncClient(closeHttp) {
        override val tracer = mock[Tracer]
      }

      closeTest.close()
      verify(closeHttp).close()
    }
    "calls close if the http client implements auto closeable" in {
      trait AutoCloser extends HttpAsyncClient with AutoCloseable
      val autoCloseHttp = mock[AutoCloser]
      val autoCloseTest = new TraceFriendlyHttpAsyncClient(autoCloseHttp) {
        override val tracer = mock[Tracer]
      }

      autoCloseTest.close()
      verify(autoCloseHttp).close()
    }
  }
}
