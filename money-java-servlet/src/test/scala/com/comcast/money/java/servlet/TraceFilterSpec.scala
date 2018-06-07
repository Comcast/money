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

import com.comcast.money.api.SpanId
import com.comcast.money.core.internal.SpanLocal
import com.comcast.money.core.Formatters.StringHexHelpers
import org.mockito.Mockito._
import org.scalatest.OptionValues._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfter, Matchers, OneInstancePerTest, WordSpec }

class TraceFilterSpec extends WordSpec with Matchers with OneInstancePerTest with BeforeAndAfter with MockitoSugar {

  val mockRequest = mock[HttpServletRequest]
  val mockResponse = mock[HttpServletResponse]
  val mockFilterChain = mock[FilterChain]
  val existingSpanId = new SpanId()
  val underTest = new TraceFilter()
  val MoneyTraceFormat = "trace-id=%s;parent-id=%s;span-id=%s"

  before {
    SpanLocal.clear()
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

    "set the trace context to the money trace header if present" in {
      when(mockRequest.getHeader("X-MoneyTrace"))
        .thenReturn(MoneyTraceFormat.format(existingSpanId.traceId, existingSpanId.parentId, existingSpanId.selfId))
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      SpanLocal.current.value.info.id shouldEqual existingSpanId
    }

    "prefer the money trace header over the X-B3 trace header" in {
      when(mockRequest.getHeader("X-MoneyTrace"))
        .thenReturn(MoneyTraceFormat.format(existingSpanId.traceId, existingSpanId.parentId, existingSpanId.selfId))
      when(mockRequest.getHeader("X-B3-TraceId"))
        .thenReturn("1234567")
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      SpanLocal.current.value.info.id shouldEqual existingSpanId
    }

    "set the trace context to the X-B3-TraceId header if present" in {
      when(mockRequest.getHeader("X-B3-TraceId"))
        .thenReturn(existingSpanId.traceId.fromGuid)
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      SpanLocal.current.value.info.id.traceId() shouldEqual existingSpanId.traceId()
    }

    "set the trace context to the X-B3-TraceId and X-B3-ParentSpanId headers if present" in {
      when(mockRequest.getHeader("X-B3-TraceId"))
        .thenReturn(existingSpanId.traceId.fromGuid)
      when(mockRequest.getHeader("X-B3-ParentSpanId"))
        .thenReturn(existingSpanId.parentId.toHexString)

      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)

      val actualSpanId = SpanLocal.current.value.info.id
      actualSpanId.traceId() shouldEqual existingSpanId.traceId()
      actualSpanId.parentId() shouldEqual existingSpanId.parentId()
    }

    "set the trace context to the X-B3-TraceId, X-B3-ParentSpanId and X-B3-SpanId headers if present" in {
      when(mockRequest.getHeader("X-B3-TraceId"))
        .thenReturn(existingSpanId.traceId.fromGuid)
      when(mockRequest.getHeader("X-B3-ParentSpanId"))
        .thenReturn(existingSpanId.parentId.toHexString)
      when(mockRequest.getHeader("X-B3-SpanId"))
        .thenReturn(existingSpanId.selfId.toHexString)
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      SpanLocal.current.value.info.id shouldEqual existingSpanId
    }

    "not set the trace context if the money trace header could not be parsed" in {
      when(mockRequest.getHeader("X-MoneyTrace")).thenReturn("can't parse this")
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      SpanLocal.current shouldBe None
    }

    "not set the trace context if the X-B3-TraceId header is not present" in {
      when(mockRequest.getHeader("X-B3-ParentSpanId"))
        .thenReturn(existingSpanId.parentId.toHexString)
      when(mockRequest.getHeader("X-B3-SpanId"))
        .thenReturn(existingSpanId.selfId.toHexString)
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      SpanLocal.current shouldBe None
    }

    "not set the trace context if the X-B3-ParentSpanId header cannot be parsed" in {
      when(mockRequest.getHeader("X-B3-TraceId"))
        .thenReturn(existingSpanId.traceId.fromGuid)
      when(mockRequest.getHeader("X-B3-ParentSpanId"))
        .thenReturn("This is not a hex number")
      when(mockRequest.getHeader("X-B3-SpanId"))
        .thenReturn(existingSpanId.selfId.toHexString)
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      SpanLocal.current shouldBe None
    }

    "not set the trace context if the X-B3-SpanId header cannot be parsed" in {
      when(mockRequest.getHeader("X-B3-TraceId"))
        .thenReturn(existingSpanId.traceId.fromGuid)
      when(mockRequest.getHeader("X-B3-ParentSpanId"))
        .thenReturn(existingSpanId.parentId().toHexString)
      when(mockRequest.getHeader("X-B3-SpanId"))
        .thenReturn("This is not a hex number")
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      SpanLocal.current shouldBe None
    }

    "set the X-B3-SpanId header if the X-B3-ParentSpanId header is not present" in {
      when(mockRequest.getHeader("X-B3-TraceId"))
        .thenReturn(existingSpanId.traceId.fromGuid)
      when(mockRequest.getHeader("X-B3-SpanId"))
        .thenReturn(existingSpanId.selfId().toHexString)
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      SpanLocal.current.value.info.id.traceId() shouldEqual existingSpanId.traceId()
      SpanLocal.current.value.info.id.parentId() shouldEqual SpanLocal.current.value.info.id.selfId()
    }

    "adds Money header to response" in {
      when(mockRequest.getHeader("X-MoneyTrace"))
        .thenReturn(MoneyTraceFormat.format(existingSpanId.traceId, existingSpanId.parentId, existingSpanId.selfId))
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      verify(mockResponse).addHeader(
        "X-MoneyTrace",
        MoneyTraceFormat.format(existingSpanId.traceId, existingSpanId.parentId, existingSpanId.selfId)
      )
    }

    "adds X-B3-TraceId header to response" in {
      when(mockRequest.getHeader("X-B3-TraceId"))
        .thenReturn(existingSpanId.traceId.fromGuid)
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      verify(mockResponse).addHeader(
        "X-B3-TraceId", existingSpanId.traceId.fromGuid
      )
    }

    "adds X-B3-TraceId and X-B3-ParentSpanId headers to response" in {
      when(mockRequest.getHeader("X-B3-TraceId"))
        .thenReturn(existingSpanId.traceId.fromGuid)
      when(mockRequest.getHeader("X-B3-ParentSpanId"))
        .thenReturn(existingSpanId.parentId.toHexString)
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      verify(mockResponse).addHeader(
        "X-B3-TraceId", existingSpanId.traceId.fromGuid
      )
      verify(mockResponse).addHeader(
        "X-B3-ParentSpanId", existingSpanId.parentId().toHexString
      )
    }

    "adds X-B3-TraceId, X-B3-ParentSpanId and X-B3-SpanId headers to response" in {
      when(mockRequest.getHeader("X-B3-TraceId"))
        .thenReturn(existingSpanId.traceId.fromGuid)
      when(mockRequest.getHeader("X-B3-ParentSpanId"))
        .thenReturn(existingSpanId.parentId.toHexString)
      when(mockRequest.getHeader("X-B3-SpanId"))
        .thenReturn(existingSpanId.selfId().toHexString)
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      verify(mockResponse).addHeader(
        "X-B3-TraceId", existingSpanId.traceId.fromGuid
      )
      verify(mockResponse).addHeader(
        "X-B3-ParentSpanId", existingSpanId.parentId().toHexString
      )
      verify(mockResponse).addHeader(
        "X-B3-SpanId", existingSpanId.selfId().toHexString
      )
    }

    "doesn't add any header to response if request does not have headers" in {
      when(mockRequest.getHeader("X-MoneyTrace")).thenReturn(null)
      when(mockRequest.getHeader("X-B3-TraceId")).thenReturn(null)
      when(mockRequest.getHeader("X-B3-ParentSpanId")).thenReturn(null)
      when(mockRequest.getHeader("X-B3-SpanId")).thenReturn(null)
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      verifyZeroInteractions(mockResponse)
    }

    "loves us some test coverage" in {
      val mockConf = mock[FilterConfig]
      underTest.init(mockConf)
      underTest.destroy()
    }
  }
}
