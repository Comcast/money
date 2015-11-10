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

package com.comcast.money.java.servlet

import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import javax.servlet.{ FilterChain, FilterConfig }

import com.comcast.money.core.SpanId
import com.comcast.money.internal.SpanLocal
import org.mockito.Mockito._
import org.scalatest.OptionValues._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfter, Matchers, OneInstancePerTest, WordSpec }

class TraceFilterSpec extends WordSpec with Matchers with OneInstancePerTest with BeforeAndAfter with MockitoSugar {

  val mockRequest = mock[HttpServletRequest]
  val mockResponse = mock[HttpServletResponse]
  val mockFilterChain = mock[FilterChain]

  val existingSpanId = SpanId()

  val underTest = new TraceFilter()

  val MoneyTraceFormat = "trace-id=%s;parent-id=%s;span-id=%s"

  before {
    SpanLocal.push(existingSpanId)
  }

  "A TraceFilter" should {
    "clear the trace context when an http request arrives" in {
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)

      SpanLocal.current shouldBe None
    }
    "always call the filter chain" in {
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      verify(mockFilterChain).doFilter(mockRequest, mockResponse)
    }
    "set the trace context to the trace header if present" in {
      when(mockRequest.getHeader("X-MoneyTrace"))
        .thenReturn(MoneyTraceFormat.format(existingSpanId.traceId, existingSpanId.parentSpanId, existingSpanId.spanId))

      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)

      SpanLocal.current.value shouldEqual existingSpanId
    }
    "not set the trace context if the trace header could not be parsed" in {
      when(mockRequest.getHeader("X-MoneyTrace")).thenReturn("can't parse this")

      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)

      SpanLocal.current shouldBe None
    }
    "adds Money header to response" in {
      when(mockRequest.getHeader("X-MoneyTrace"))
        .thenReturn(MoneyTraceFormat.format(existingSpanId.traceId, existingSpanId.parentSpanId, existingSpanId.spanId))
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      verify(mockResponse).addHeader(
        "X-MoneyTrace",
        MoneyTraceFormat.format(existingSpanId.traceId, existingSpanId.parentSpanId, existingSpanId.spanId)
      )
    }
    "doesn't add Money header to response if response is null" in {
      when(mockRequest.getHeader("X-MoneyTrace"))
        .thenReturn(MoneyTraceFormat.format(existingSpanId.traceId, existingSpanId.parentSpanId, existingSpanId.spanId))
      underTest.doFilter(mockRequest, null, mockFilterChain)
      verifyZeroInteractions(mockResponse)
    }
    "doesn't add Money header to response if request does not have an X-MoneyTrace header" in {
      when(mockRequest.getHeader("X-MoneyTrace")).thenReturn(null)
      underTest.doFilter(mockRequest, null, mockFilterChain)
      verifyZeroInteractions(mockResponse)
    }
    "loves us some test coverage" in {
      val mockConf = mock[FilterConfig]
      underTest.init(mockConf)
      underTest.destroy()
    }
  }
}
