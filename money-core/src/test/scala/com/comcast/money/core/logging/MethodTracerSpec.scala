package com.comcast.money.core.logging

import java.lang.reflect.Method

import com.comcast.money.annotations.{Timed, Traced, TracedData}
import com.comcast.money.api.Span
import com.comcast.money.core.Tracer
import com.comcast.money.core.async.{AsyncNotificationHandler, AsyncNotifier}
import com.comcast.money.core.internal.{MDCSupport, SpanContext}
import org.mockito.Matchers.{any => argAny}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}

import scala.util.{Failure, Success, Try}

class MethodTracerSpec extends WordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  val mockTracer: Tracer = mock[Tracer]
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

        when(proceed.apply()).thenReturn("result")

        val result = methodTracer.traceMethod(method, traced, empty, proceed)

        verify(mockTracer).startSpan(traced.value())
        verify(mockTracer).stopSpan(true)

        result shouldBe "result"
      }
      "that throws" in {

        val method = tracedMethod
        val traced = method.getDeclaredAnnotation(classOf[Traced])

        val empty = Array.empty[AnyRef]
        val proceed = mock[() => String]

        when(proceed.apply()).thenThrow(new IllegalArgumentException())

        assertThrows[IllegalArgumentException] {
          methodTracer.traceMethod(method, traced, empty, proceed)
        }

        verify(mockTracer).startSpan(traced.value())
        verify(mockTracer).stopSpan(false)
      }
      "that throws an ignored exception" in {
        val method = tracedMethodWithIgnoredExceptions
        val traced = method.getDeclaredAnnotation(classOf[Traced])

        val empty = Array.empty[AnyRef]
        val proceed = mock[() => String]

        when(proceed.apply()).thenThrow(new IllegalArgumentException())

        assertThrows[IllegalArgumentException] {
          methodTracer.traceMethod(method, traced, empty, proceed)
        }

        verify(mockTracer).startSpan(traced.value())
        verify(mockTracer).stopSpan(true)
      }
    }
    "traces async methods" should {
      "that complete successfully" in {
        val method = tracedAsyncMethod
        val traced = method.getDeclaredAnnotation(classOf[Traced])

        val empty = Array.empty[AnyRef]
        val proceed = mock[() => String]
        val handler = new TestAsyncHandler("result2")

        when(proceed.apply()).thenReturn("result")
        when(mockAsyncNotifier.resolveHandler(classOf[String], "result")).thenReturn(Some(handler))

        val span = mock[Span]
        when(mockSpanContext.pop).thenReturn(Some(span))
        val result = methodTracer.traceMethod(method, traced, empty, proceed)

        verify(mockTracer).startSpan(traced.value())
        verify(mockSpanContext).pop
        verify(mockTracer, never()).stopSpan(argAny())

        result shouldBe "result2"

        handler.callback(Success("result3"))

        verify(span).stop(true)
      }
      "that complete exceptionally" in {
        val method = tracedAsyncMethod
        val traced = method.getDeclaredAnnotation(classOf[Traced])

        val empty = Array.empty[AnyRef]
        val proceed = mock[() => String]
        val handler = new TestAsyncHandler("result2")

        when(proceed.apply()).thenReturn("result")
        when(mockAsyncNotifier.resolveHandler(classOf[String], "result")).thenReturn(Some(handler))

        val span = mock[Span]
        when(mockSpanContext.pop).thenReturn(Some(span))
        val result = methodTracer.traceMethod(method, traced, empty, proceed)

        verify(mockTracer).startSpan(traced.value())
        verify(mockSpanContext).pop
        verify(mockTracer, never()).stopSpan(argAny())

        result shouldBe "result2"

        handler.callback(Failure(new Exception))

        verify(span).stop(false)
      }
      "that complete exceptionally with ignored exception" in {
        val method = tracedAsyncMethodWithIgnoredExceptions
        val traced = method.getDeclaredAnnotation(classOf[Traced])

        val empty = Array.empty[AnyRef]
        val proceed = mock[() => String]
        val handler = new TestAsyncHandler("result2")

        when(proceed.apply()).thenReturn("result")
        when(mockAsyncNotifier.resolveHandler(classOf[String], "result")).thenReturn(Some(handler))

        val span = mock[Span]
        when(mockSpanContext.pop).thenReturn(Some(span))
        val result = methodTracer.traceMethod(method, traced, empty, proceed)

        verify(mockTracer).startSpan(traced.value())
        verify(mockSpanContext).pop
        verify(mockTracer, never()).stopSpan(argAny())

        result shouldBe "result2"

        handler.callback(Failure(new IllegalArgumentException()))

        verify(span).stop(true)
      }
      "with unhandled return value" in {
        val method = tracedAsyncMethod
        val traced = method.getDeclaredAnnotation(classOf[Traced])

        val empty = Array.empty[AnyRef]
        val proceed = mock[() => String]
        val handler = new TestAsyncHandler("result2")

        when(proceed.apply()).thenReturn("result")
        when(mockAsyncNotifier.resolveHandler(classOf[String], "result")).thenReturn(None)

        val result = methodTracer.traceMethod(method, traced, empty, proceed)

        verify(mockTracer).startSpan(traced.value())
        verify(mockTracer).stopSpan(true)

        result shouldBe "result"
      }
      "that throws" in {
        val method = tracedAsyncMethod
        val traced = method.getDeclaredAnnotation(classOf[Traced])

        val empty = Array.empty[AnyRef]
        val proceed = mock[() => String]
        val handler = new TestAsyncHandler("result2")

        when(proceed.apply()).thenThrow(new IllegalArgumentException())

        assertThrows[IllegalArgumentException] {
          methodTracer.traceMethod(method, traced, empty, proceed)
        }

        verify(mockTracer).startSpan(traced.value())
        verify(mockTracer).stopSpan(false)
      }
    }
    "time method" should {
      "that return successfully" in {

        val method = timedMethod
        val timed = method.getDeclaredAnnotation(classOf[Timed])

        val proceed = mock[() => String]

        when(proceed.apply()).thenReturn("result")

        val result = methodTracer.timeMethod(method, timed, proceed)

        verify(mockTracer).startTimer(timed.value())
        verify(mockTracer).stopTimer(timed.value())

        result shouldBe "result"
      }
      "that throws" in {

        val method = timedMethod
        val timed = method.getDeclaredAnnotation(classOf[Timed])

        val proceed = mock[() => String]

        when(proceed.apply()).thenThrow(new IllegalArgumentException())

        assertThrows[IllegalArgumentException] {
          methodTracer.timeMethod(method, timed, proceed)
        }

        verify(mockTracer).startTimer(timed.value())
        verify(mockTracer).stopTimer(timed.value())
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