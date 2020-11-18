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

package com.comcast.money.core.logging

import java.lang.reflect.Method

import com.comcast.money.annotations.{ Timed, Traced, TracedData }
import com.comcast.money.api.{ Span, SpanBuilder }
import com.comcast.money.core.Tracer
import com.comcast.money.core.async.{ AsyncNotificationHandler, AsyncNotifier }
import com.comcast.money.core.internal.{ MDCSupport, SpanContext }
import io.opentelemetry.context.{ Context, Scope }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._

import scala.util.{ Failure, Success, Try }
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.OneInstancePerTest

class MethodTracerSpec extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  val mockTracer: Tracer = mock[Tracer]
  val mockSpanBuilder: SpanBuilder = mock[SpanBuilder]
  val mockSpan: Span = mock[Span]
  val mockContext: Context = mock[Context]
  val mockScope: Scope = mock[Scope]
  val mockAsyncNotifier: AsyncNotifier = mock[AsyncNotifier]
  val mockMdcSupport: MDCSupport = mock[MDCSupport]
  val mockSpanContext: SpanContext = mock[SpanContext]

  val tracedMethod: Method = classOf[Samples].getMethod("tracedMethod")
  val tracedMethodWithParameters: Method = classOf[Samples].getMethod("tracedMethod")
  val tracedMethodWithIgnoredExceptions: Method = classOf[Samples].getMethod("tracedMethodWithIgnoredExceptions")
  val tracedAsyncMethod: Method = classOf[Samples].getMethod("tracedAsyncMethod")
  val tracedAsyncMethodWithIgnoredExceptions: Method = classOf[Samples].getMethod("tracedAsyncMethodWithIgnoredExceptions")
  val timedMethod: Method = classOf[Samples].getMethod("timedMethod")

  val methodTracer: MethodTracer = new MethodTracer {
    override val tracer: Tracer = mockTracer
    override val asyncNotifier: AsyncNotifier = mockAsyncNotifier
    override val mdcSupport: MDCSupport = mockMdcSupport
    override val spanContext: SpanContext = mockSpanContext
  }

  "MethodTracer" should {
    "trace methods" should {
      "that return successfully" in {

        val method = tracedMethod
        val traced = method.getDeclaredAnnotation(classOf[Traced])

        val empty = Array.empty[AnyRef]
        val proceed = mock[() => String]

        when(mockTracer.spanBuilder(traced.value())).thenReturn(mockSpanBuilder)
        when(mockSpanBuilder.startSpan()).thenReturn(mockSpan)
        when(mockSpan.storeInContext(any())).thenReturn(mockContext)
        when(mockContext.makeCurrent()).thenReturn(mockScope)
        when(proceed.apply()).thenReturn("result")

        val result = methodTracer.traceMethod(method, traced, empty, proceed)

        verify(mockTracer).spanBuilder(traced.value())
        verify(mockSpanBuilder).startSpan()
        verify(mockSpan).storeInContext(any())
        verify(mockContext).makeCurrent()
        verify(mockSpan).stop(true)
        verify(mockScope).close()

        result shouldBe "result"
      }
      "that throws" in {

        val method = tracedMethod
        val traced = method.getDeclaredAnnotation(classOf[Traced])

        val empty = Array.empty[AnyRef]
        val proceed = mock[() => String]

        when(mockTracer.spanBuilder(traced.value())).thenReturn(mockSpanBuilder)
        when(mockSpanBuilder.startSpan()).thenReturn(mockSpan)
        when(mockSpan.storeInContext(any())).thenReturn(mockContext)
        when(mockContext.makeCurrent()).thenReturn(mockScope)
        when(proceed.apply()).thenThrow(new IllegalArgumentException())

        assertThrows[IllegalArgumentException] {
          methodTracer.traceMethod(method, traced, empty, proceed)
        }

        verify(mockTracer).spanBuilder(traced.value())
        verify(mockSpanBuilder).startSpan()
        verify(mockSpan).storeInContext(any())
        verify(mockContext).makeCurrent()
        verify(mockSpan).stop(false)
        verify(mockScope).close()
      }
      "that throws an ignored exception" in {
        val method = tracedMethodWithIgnoredExceptions
        val traced = method.getDeclaredAnnotation(classOf[Traced])

        val empty = Array.empty[AnyRef]
        val proceed = mock[() => String]

        when(mockTracer.spanBuilder(traced.value())).thenReturn(mockSpanBuilder)
        when(mockSpanBuilder.startSpan()).thenReturn(mockSpan)
        when(mockSpan.storeInContext(any())).thenReturn(mockContext)
        when(mockContext.makeCurrent()).thenReturn(mockScope)
        when(proceed.apply()).thenThrow(new IllegalArgumentException())

        assertThrows[IllegalArgumentException] {
          methodTracer.traceMethod(method, traced, empty, proceed)
        }

        verify(mockTracer).spanBuilder(traced.value())
        verify(mockSpanBuilder).startSpan()
        verify(mockSpan).storeInContext(any())
        verify(mockContext).makeCurrent()
        verify(mockSpan).stop(true)
        verify(mockScope).close()
      }
    }
    "traces async methods" should {
      "that complete successfully" in {
        val method = tracedAsyncMethod
        val traced = method.getDeclaredAnnotation(classOf[Traced])

        val empty = Array.empty[AnyRef]
        val proceed = mock[() => String]
        val handler = new TestAsyncHandler("result2")

        when(mockTracer.spanBuilder(traced.value())).thenReturn(mockSpanBuilder)
        when(mockSpanBuilder.startSpan()).thenReturn(mockSpan)
        when(mockSpan.storeInContext(any())).thenReturn(mockContext)
        when(mockContext.makeCurrent()).thenReturn(mockScope)
        when(proceed.apply()).thenReturn("result")
        when(mockAsyncNotifier.resolveHandler(classOf[String], "result")).thenReturn(Some(handler))

        val result = methodTracer.traceMethod(method, traced, empty, proceed)

        verify(mockTracer).spanBuilder(traced.value())
        verify(mockSpanBuilder).startSpan()
        verify(mockSpan).storeInContext(any())
        verify(mockContext).makeCurrent()
        verify(mockSpan, never()).stop(any())
        verify(mockScope).close()

        result shouldBe "result2"

        handler.callback(Success("result3"))

        verify(mockSpan).stop(true)
      }
      "that complete exceptionally" in {
        val method = tracedAsyncMethod
        val traced = method.getDeclaredAnnotation(classOf[Traced])

        val empty = Array.empty[AnyRef]
        val proceed = mock[() => String]
        val handler = new TestAsyncHandler("result2")

        when(mockTracer.spanBuilder(traced.value())).thenReturn(mockSpanBuilder)
        when(mockSpanBuilder.startSpan()).thenReturn(mockSpan)
        when(mockSpan.storeInContext(any())).thenReturn(mockContext)
        when(mockContext.makeCurrent()).thenReturn(mockScope)
        when(proceed.apply()).thenReturn("result")
        when(mockAsyncNotifier.resolveHandler(classOf[String], "result")).thenReturn(Some(handler))

        val result = methodTracer.traceMethod(method, traced, empty, proceed)

        verify(mockTracer).spanBuilder(traced.value())
        verify(mockSpanBuilder).startSpan()
        verify(mockSpan).storeInContext(any())
        verify(mockContext).makeCurrent()
        verify(mockSpan, never()).stop(any())
        verify(mockScope).close()

        result shouldBe "result2"

        handler.callback(Failure(new Exception))

        verify(mockSpan).stop(false)
      }
      "that complete exceptionally with ignored exception" in {
        val method = tracedAsyncMethodWithIgnoredExceptions
        val traced = method.getDeclaredAnnotation(classOf[Traced])

        val empty = Array.empty[AnyRef]
        val proceed = mock[() => String]
        val handler = new TestAsyncHandler("result2")

        when(mockTracer.spanBuilder(traced.value())).thenReturn(mockSpanBuilder)
        when(mockSpanBuilder.startSpan()).thenReturn(mockSpan)
        when(mockSpan.storeInContext(any())).thenReturn(mockContext)
        when(mockContext.makeCurrent()).thenReturn(mockScope)
        when(proceed.apply()).thenReturn("result")
        when(mockAsyncNotifier.resolveHandler(classOf[String], "result")).thenReturn(Some(handler))

        val result = methodTracer.traceMethod(method, traced, empty, proceed)

        verify(mockTracer).spanBuilder(traced.value())
        verify(mockSpanBuilder).startSpan()
        verify(mockSpan).storeInContext(any())
        verify(mockContext).makeCurrent()
        verify(mockSpan, never()).stop(any())
        verify(mockScope).close()

        result shouldBe "result2"

        handler.callback(Failure(new IllegalArgumentException()))

        verify(mockSpan).stop(true)
      }
      "with unhandled return value" in {
        val method = tracedAsyncMethod
        val traced = method.getDeclaredAnnotation(classOf[Traced])

        val empty = Array.empty[AnyRef]
        val proceed = mock[() => String]

        when(mockTracer.spanBuilder(traced.value())).thenReturn(mockSpanBuilder)
        when(mockSpanBuilder.startSpan()).thenReturn(mockSpan)
        when(mockSpan.storeInContext(any())).thenReturn(mockContext)
        when(mockContext.makeCurrent()).thenReturn(mockScope)
        when(proceed.apply()).thenReturn("result")
        when(mockAsyncNotifier.resolveHandler(classOf[String], "result")).thenReturn(None)

        val result = methodTracer.traceMethod(method, traced, empty, proceed)

        verify(mockTracer).spanBuilder(traced.value())
        verify(mockSpanBuilder).startSpan()
        verify(mockSpan).storeInContext(any())
        verify(mockContext).makeCurrent()
        verify(mockSpan).stop(true)
        verify(mockScope).close()

        result shouldBe "result"
      }
      "that throws" in {
        val method = tracedAsyncMethod
        val traced = method.getDeclaredAnnotation(classOf[Traced])

        val empty = Array.empty[AnyRef]
        val proceed = mock[() => String]
        val handler = new TestAsyncHandler("result2")

        when(mockTracer.spanBuilder(traced.value())).thenReturn(mockSpanBuilder)
        when(mockSpanBuilder.startSpan()).thenReturn(mockSpan)
        when(mockSpan.storeInContext(any())).thenReturn(mockContext)
        when(mockContext.makeCurrent()).thenReturn(mockScope)
        when(proceed.apply()).thenThrow(new IllegalArgumentException())

        assertThrows[IllegalArgumentException] {
          methodTracer.traceMethod(method, traced, empty, proceed)
        }

        verify(mockTracer).spanBuilder(traced.value())
        verify(mockSpanBuilder).startSpan()
        verify(mockSpan).storeInContext(any())
        verify(mockContext).makeCurrent()
        verify(mockSpan).stop(false)
        verify(mockScope).close()
      }
    }
    "time method" should {
      "that return successfully" in {

        val method = timedMethod
        val timed = method.getDeclaredAnnotation(classOf[Timed])

        val proceed = mock[() => String]

        when(mockTracer.startTimer(timed.value())).thenReturn(mockScope)
        when(proceed.apply()).thenReturn("result")

        val result = methodTracer.timeMethod(method, timed, proceed)

        verify(mockTracer).startTimer(timed.value())
        verify(mockScope).close()

        result shouldBe "result"
      }
      "that throws" in {

        val method = timedMethod
        val timed = method.getDeclaredAnnotation(classOf[Timed])

        val proceed = mock[() => String]

        when(mockTracer.startTimer(timed.value())).thenReturn(mockScope)
        when(proceed.apply()).thenThrow(new IllegalArgumentException())

        assertThrows[IllegalArgumentException] {
          methodTracer.timeMethod(method, timed, proceed)
        }

        verify(mockTracer).startTimer(timed.value())
        verify(mockScope).close()
      }
    }
  }

  class TestAsyncHandler(result: String, supports: Boolean = true) extends AsyncNotificationHandler {
    var callback: Try[_] => Unit = _

    override def supports(futureClass: Class[_], future: AnyRef): Boolean = supports
    override def whenComplete(futureClass: Class[_], future: AnyRef)(f: Try[_] => Unit): AnyRef = {
      callback = f
      result
    }
  }

  class Samples {
    @Traced(value = "tracedMethod")
    def tracedMethod(): String = "tracedMethod"
    @Traced(value = "tracedMethodWithParameters")
    def tracedMethodWithParameters(@TracedData("foo") foo: String, @TracedData("foo") bar: String): String = "tracedMethodWithParameters"
    @Traced(value = "tracedMethodWithIgnoredExceptions", ignoredExceptions = Array(classOf[IllegalArgumentException]))
    def tracedMethodWithIgnoredExceptions(): String = "tracedMethodWithIgnoredExceptions"
    @Traced(value = "tracedAsyncMethod", async = true)
    def tracedAsyncMethod(): String = "tracedAsyncMethod"
    @Traced(value = "tracedAsyncMethodWithIgnoredExceptions", async = true, ignoredExceptions = Array(classOf[IllegalArgumentException]))
    def tracedAsyncMethodWithIgnoredExceptions(): String = "tracedAsyncMethodWithIgnoredExceptions"

    @Timed(value = "timedMethod")
    def timedMethod(): String = "timedMethod"
  }
}