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

import com.comcast.money.api.SpanId
import com.comcast.money.core.{ Formatters => CoreSpanId, SpecHelpers, Tracer }
import com.comcast.money.core.internal.SpanLocal
import org.apache.http.client.methods.{ CloseableHttpResponse, HttpUriRequest }
import org.apache.http.client.{ HttpClient, ResponseHandler }
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.protocol.HttpContext
import org.apache.http.{ HttpHost, HttpResponse, StatusLine }
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.mock.MockitoSugar

class TraceFriendlyHttpClientSpec extends WordSpec with SpecHelpers
    with Matchers with MockitoSugar with OneInstancePerTest with BeforeAndAfterEach {

  val httpClient = mock[CloseableHttpClient]
  val httpUriRequest = mock[HttpUriRequest]
  val httpResponse = mock[CloseableHttpResponse]
  val statusLine = mock[StatusLine]
  val httpHost = new HttpHost("localhost")
  val httpContext = mock[HttpContext]
  val spanId = new SpanId()

  when(httpResponse.getStatusLine).thenReturn(statusLine)
  when(statusLine.getStatusCode).thenReturn(200)

  val testHttpResponseHandler = new ResponseHandler[String] {
    override def handleResponse(response: HttpResponse): String = "response-handler-test"
  }

  // extend what we are testing so we can use a mock tracer
  val underTest = new TraceFriendlyHttpClient(httpClient) {
    override val tracer = mock[Tracer]
  }

  override def beforeEach(): Unit = {
    SpanLocal.push(testSpan(spanId))
  }

  // if you don't reset, then the verifies are going to be off
  override def afterEach() = {
    reset(underTest.tracer, httpUriRequest, httpClient)
    SpanLocal.clear()
  }

  def verifyTracing() = {
    verify(underTest.tracer).startTimer(HttpTraceConfig.HttpResponseTimeTraceKey)
    verify(underTest.tracer).stopTimer(HttpTraceConfig.HttpResponseTimeTraceKey)
    verify(underTest.tracer).record("http-response-code", 200L)
    verify(httpUriRequest).setHeader("X-MoneyTrace", CoreSpanId.toHttpHeader(spanId))
    verify(httpUriRequest).setHeader("X-B3-TraceId", spanId.traceId.replace("-", ""))
    //    verify(httpUriRequest).setHeader("X-B3-ParentSpanId", spanId.parentId().t oHexString) // Does not get set because this is a root span
    verify(httpUriRequest).setHeader("X-B3-SpanId", spanId.selfId().toHexString)
  }

  "TraceFriendlyHttpClient" should {
    "simply call the wrapped client getParams" in {
      underTest.getParams
      verify(httpClient).getParams
    }
    "simply call the wrapped client getConnectionManager" in {
      underTest.getConnectionManager
      verify(httpClient).getConnectionManager
    }
    "record the status code and call duration when execute(HttpUriRequest)" in {
      when(httpClient.execute(httpUriRequest)).thenReturn(httpResponse)
      underTest.execute(httpUriRequest)
      verifyTracing()
    }
    "record the status code and call duration when execute(HttpUriRequest, HttpContext)" in {
      when(httpClient.execute(httpUriRequest, httpContext)).thenReturn(httpResponse)
      underTest.execute(httpUriRequest, httpContext)
      verifyTracing()
    }
    "record the status code and call duration when execute(HttpHost, HttpRequest)" in {
      when(httpClient.execute(httpHost, httpUriRequest)).thenReturn(httpResponse)
      underTest.execute(httpHost, httpUriRequest)
      verifyTracing()
    }
    "record the status code and call duration when execute(HttpHost, HttpRequest, HttpContext)" in {
      when(httpClient.execute(httpHost, httpUriRequest, httpContext)).thenReturn(httpResponse)
      underTest.execute(httpHost, httpUriRequest, httpContext)
      verifyTracing()
    }
    "simply call the wrapped client when execute(HttpUriRequest, ResponseHandler)" in {
      underTest.execute(httpUriRequest, testHttpResponseHandler)
      verify(httpClient).execute(httpUriRequest, testHttpResponseHandler)
    }
    "simply call the wrapped client when execute(HttpUriRequest, ResponseHandler, HttpContext)" in {
      underTest.execute(httpUriRequest, testHttpResponseHandler, httpContext)
      verify(httpClient).execute(httpUriRequest, testHttpResponseHandler, httpContext)
    }
    "simply call the wrapped client when execute(HttpHost, HttpRequest, ResponseHandler)" in {
      underTest.execute(httpHost, httpUriRequest, testHttpResponseHandler)
      verify(httpClient).execute(httpHost, httpUriRequest, testHttpResponseHandler)
    }
    "simply call the wrapped client when execute(HttpHost, HttpRequest, ResponseHandler, HttpContext)" in {
      underTest.execute(httpHost, httpUriRequest, testHttpResponseHandler, httpContext)
      verify(httpClient).execute(httpHost, httpUriRequest, testHttpResponseHandler, httpContext)
    }
    "records a zero for a status code on exception" in {
      when(httpClient.execute(httpUriRequest)).thenThrow(new RuntimeException("bad"))
      intercept[RuntimeException] {
        underTest.execute(httpUriRequest)
      }
      verify(underTest.tracer).record("http-response-code", 0L)
    }
    "calls close on closeable http client" in {
      underTest.close()
      verify(httpClient).close()
    }
    "calls close if the http client implements closable" in {
      trait Closer extends HttpClient with Closeable

      val closeHttp = mock[Closer]
      val closeTest = new TraceFriendlyHttpClient(closeHttp) {
        override val tracer = mock[Tracer]
      }

      closeTest.close()
      verify(closeHttp).close()
    }
    "calls close if the http client implements auto closeable" in {
      trait AutoCloser extends HttpClient with AutoCloseable
      val autoCloseHttp = mock[AutoCloser]
      val autoCloseTest = new TraceFriendlyHttpClient(autoCloseHttp) {
        override val tracer = mock[Tracer]
      }

      autoCloseTest.close()
      verify(autoCloseHttp).close()
    }
  }
}
