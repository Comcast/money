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

package com.comcast.money.http.client

import java.io.Closeable

import com.comcast.money.api.SpanId
import com.comcast.money.core.{ SpecHelpers, Tracer }
import com.comcast.money.core.internal.SpanLocal
import io.opentelemetry.context.Scope
import org.apache.http.client.methods.{ CloseableHttpResponse, HttpUriRequest }
import org.apache.http.client.{ HttpClient, ResponseHandler }
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.protocol.HttpContext
import org.apache.http.{ HttpHost, HttpResponse, StatusLine }
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class TraceFriendlyHttpClientSpec extends AnyWordSpec with SpecHelpers
  with Matchers with MockitoSugar with OneInstancePerTest with BeforeAndAfterEach {

  val httpClient = mock[CloseableHttpClient]
  val httpUriRequest = mock[HttpUriRequest]
  val httpResponse = mock[CloseableHttpResponse]
  val statusLine = mock[StatusLine]
  val httpHost = new HttpHost("localhost")
  val httpContext = mock[HttpContext]
  val spanId = SpanId.createNew()
  val scope = mock[Scope]

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
    when(underTest.tracer.startTimer(anyString())).thenReturn(scope)
  }

  // if you don't reset, then the verifies are going to be off
  override def afterEach() = {
    reset(underTest.tracer, httpUriRequest, httpClient)
    SpanLocal.clear()
  }

  def verifyTracing() = {
    verify(underTest.tracer).startTimer(HttpTraceConfig.HttpResponseTimeTraceKey)
    verify(scope).close()
    verify(underTest.tracer).record("http-response-code", 200L)
    verify(httpUriRequest).setHeader("X-MoneyTrace", s"trace-id=${spanId.traceId};parent-id=${spanId.parentId};span-id=${spanId.selfId}")
    verify(httpUriRequest).setHeader("traceparent", f"00-${spanId.traceId.replace("-", "")}%s-${spanId.selfId}%016x-01")
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
