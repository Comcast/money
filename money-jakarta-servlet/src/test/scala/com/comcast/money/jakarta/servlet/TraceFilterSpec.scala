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

package com.comcast.money.jakarta.servlet

import com.comcast.money.api.{ Span, SpanId }
import com.comcast.money.core.formatters.FormatterUtils.randomRemoteSpanId
import com.comcast.money.core.internal.SpanLocal
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.OptionValues._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{ BeforeAndAfter, OneInstancePerTest }
import org.scalatestplus.mockito.MockitoSugar

import java.util.Collections
import jakarta.servlet.http.{ HttpServletRequest, HttpServletResponse }
import jakarta.servlet.{ FilterChain, FilterConfig, ServletRequest, ServletResponse }

class TraceFilterSpec extends AnyWordSpec with Matchers with OneInstancePerTest with BeforeAndAfter with MockitoSugar {

  val mockRequest = mock[HttpServletRequest]
  val mockResponse = mock[HttpServletResponse]
  val mockFilterChain = mock[FilterChain]
  val existingSpanId = randomRemoteSpanId()
  val underTest = new TraceFilter()
  val MoneyTraceFormat = "trace-id=%s;parent-id=%s;span-id=%s"
  val filterChain: FilterChain = (_: ServletRequest, _: ServletResponse) => capturedSpan = SpanLocal.current
  var capturedSpan: Option[Span] = None

  def traceParentHeader(spanId: SpanId): String = {
    val traceId = spanId.traceId.replace("-", "").toLowerCase
    f"00-$traceId%s-${spanId.selfId}%016x-00"
  }

  before {
    capturedSpan = None
    val empty: java.util.Enumeration[_] = Collections.emptyEnumeration()
    // The raw type seems to confuse the Scala compiler so the cast is required to compile successfully
    when(mockRequest.getHeaderNames).asInstanceOf[OngoingStubbing[java.util.Enumeration[_]]].thenReturn(empty)
  }

  "A TraceFilter" should {
    "clear the trace context when an http request arrives" in {
      underTest.doFilter(mockRequest, mockResponse, filterChain)
      SpanLocal.current shouldBe None
    }

    "always call the filter chain" in {
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      verify(mockFilterChain).doFilter(mockRequest, mockResponse)
    }

    "set the trace context to the money trace header if present" in {
      when(mockRequest.getHeader("X-MoneyTrace"))
        .thenReturn(MoneyTraceFormat.format(existingSpanId.traceId, existingSpanId.parentId, existingSpanId.selfId))
      underTest.doFilter(mockRequest, mockResponse, filterChain)
      capturedSpan.value.info.id shouldEqual existingSpanId
    }

    "set the trace context to the traceparent header if present" in {
      when(mockRequest.getHeader("traceparent"))
        .thenReturn(traceParentHeader(existingSpanId))
      underTest.doFilter(mockRequest, mockResponse, filterChain)

      val actualSpanId = capturedSpan.value.info.id
      actualSpanId.traceId shouldEqual existingSpanId.traceId
      actualSpanId.parentId shouldEqual existingSpanId.selfId
    }

    "prefer the money trace header over the W3C Trace Context header" in {
      when(mockRequest.getHeader("X-MoneyTrace"))
        .thenReturn(MoneyTraceFormat.format(existingSpanId.traceId, existingSpanId.parentId, existingSpanId.selfId))
      when(mockRequest.getHeader("traceparent"))
        .thenReturn(traceParentHeader(SpanId.createNew()))
      underTest.doFilter(mockRequest, mockResponse, filterChain)
      capturedSpan.value.info.id shouldEqual existingSpanId
    }

    "not set the trace context if the money trace header could not be parsed" in {
      when(mockRequest.getHeader("X-MoneyTrace")).thenReturn("can't parse this")
      underTest.doFilter(mockRequest, mockResponse, filterChain)
      capturedSpan shouldBe None
    }

    "adds Money header to response" in {
      when(mockRequest.getHeader("X-MoneyTrace"))
        .thenReturn(MoneyTraceFormat.format(existingSpanId.traceId, existingSpanId.parentId, existingSpanId.selfId))
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      verify(mockResponse).addHeader(
        "X-MoneyTrace",
        MoneyTraceFormat.format(existingSpanId.traceId, existingSpanId.parentId, existingSpanId.selfId))
    }

    "adds Trace Context header to response" in {
      when(mockRequest.getHeader("traceparent"))
        .thenReturn(traceParentHeader(existingSpanId))
      underTest.doFilter(mockRequest, mockResponse, mockFilterChain)
      verify(mockResponse).addHeader(
        "traceparent",
        traceParentHeader(existingSpanId))
    }

    "loves us some test coverage" in {
      val mockConf = mock[FilterConfig]
      underTest.init(mockConf)
      underTest.destroy()
    }
  }
}
