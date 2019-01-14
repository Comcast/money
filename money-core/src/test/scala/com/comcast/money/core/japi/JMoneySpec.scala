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

package com.comcast.money.core.japi

import com.comcast.money.api.{ Note, Span, SpanFactory, SpanId }
import com.comcast.money.core.handlers.TestData
import com.comcast.money.core.Tracer
import com.comcast.money.core.internal.SpanLocal
import com.comcast.money.core.japi.JMoney._
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.Mockito.doReturn
import org.scalatest.mockito.MockitoSugar
import org.scalatest._

class JMoneySpec extends WordSpec
  with Matchers with MockitoSugar with TestData with BeforeAndAfterEach with OneInstancePerTest {

  val tracer = mock[Tracer]

  override def beforeEach() = {
    SpanLocal.clear()
    reset(tracer)
    JMoney.setTracer(tracer)
  }

  override def afterEach() = {
    SpanLocal.clear()
    JMoney.setTracer(null)
  }

  "Java API" should {
    "starts a span" in {
      JMoney.startSpan("java-span")
      verify(tracer).startSpan("java-span")
    }
    "stops a span" in {
      JMoney.stopSpan(true)
      verify(tracer).stopSpan(true)
    }
    "support closeable" in {
      val span: TraceSpan = JMoney.newSpan("closeable")
      span.close()
      span.close()

      verify(tracer).startSpan("closeable")
      verify(tracer, times(1)).stopSpan(true)
    }
    "support failed closeable" in {
      val span: TraceSpan = JMoney.newSpan("closeable")
      span.fail()
      span.close()
      span.close()

      verify(tracer).startSpan("closeable")
      verify(tracer, times(1)).stopSpan(false)
    }
    "support closeable with failed default" in {
      val span: TraceSpan = JMoney.newSpan("closeable", false)
      span.close()
      span.close()

      verify(tracer).startSpan("closeable")
      verify(tracer, times(1)).stopSpan(false)
    }
    "support successful closeable with failed default" in {
      val span: TraceSpan = JMoney.newSpan("closeable", false)
      span.success()
      span.close()
      span.close()

      verify(tracer).startSpan("closeable")
      verify(tracer, times(1)).stopSpan(true)
    }
    "support runnable lambda" in {
      val runnable = mock[CheckedRunnable[Exception]]
      JMoney.trace("runnable", runnable)

      verify(tracer).startSpan("runnable")
      verify(runnable).run()
      verify(tracer, times(1)).stopSpan(true)
    }
    "support throwing runnable lambda" in {
      val runnable = mock[CheckedRunnable[Exception]]
      doThrow(new Exception()).when(runnable).run()

      assertThrows[Exception] {
        JMoney.trace("runnable", runnable)
      }

      verify(tracer).startSpan("runnable")
      verify(runnable).run()
      verify(tracer, times(1)).stopSpan(false)
    }
    "support consumer lambda with span" in {
      val consumer = mock[CheckedConsumer[TraceSpan, Exception]]

      JMoney.trace("consumer", consumer)

      verify(tracer).startSpan("consumer")
      verify(consumer).accept(any())
      verify(tracer).stopSpan(true)
    }
    "support consumer lambda with failed span" in {
      val consumer: CheckedConsumer[TraceSpan, Exception] = span => { span.fail() }

      JMoney.trace("consumer", consumer)

      verify(tracer).startSpan("consumer")
      verify(tracer).stopSpan(false)
    }
    "support throwing consumer lambda" in {
      val consumer = mock[CheckedConsumer[TraceSpan, Exception]]
      doThrow(new Exception()).when(consumer).accept(any())

      assertThrows[Exception] {
        JMoney.trace("consumer", consumer)
      }

      verify(tracer).startSpan("consumer")
      verify(consumer).accept(any())
      verify(tracer).stopSpan(false)
    }
    "support callable lambda" in {
      val callable = mock[CheckedCallable[String, Exception]]
      doReturn("Hello").when(callable).call()
      val result = JMoney.trace("callable", callable)

      result shouldBe "Hello"

      verify(tracer).startSpan("callable")
      verify(callable).call()
      verify(tracer).stopSpan(true)
    }
    "support throwing callable lambda" in {
      val callable = mock[CheckedCallable[String, Exception]]
      doThrow(new Exception()).when(callable).call()
      assertThrows[Exception] {
        JMoney.trace("callable", callable)
      }

      verify(tracer).startSpan("callable")
      verify(callable).call
      verify(tracer).stopSpan(false)
    }
    "support function lambda with span" in {
      val function = mock[CheckedFunction[TraceSpan, String, Exception]]
      doReturn("Hello").when(function).apply(any())

      val result = JMoney.trace("function", function)

      result shouldBe "Hello"

      verify(tracer).startSpan("function")
      verify(function).apply(any())
      verify(tracer).stopSpan(true)
    }
    "support function lambda with failed span" in {
      val function: CheckedFunction[TraceSpan, String, Exception] = span => {
        span.fail()
        "Hello"
      }

      val result = JMoney.trace("function", function)

      result shouldBe "Hello"

      verify(tracer).startSpan("function")
      verify(tracer).stopSpan(false)
    }
    "support throwing function lambda" in {
      val function = mock[CheckedFunction[TraceSpan, String, Exception]]
      doThrow(new Exception()).when(function).apply(any())

      assertThrows[Exception] {
        JMoney.trace("function", function)
      }

      verify(tracer).startSpan("function")
      verify(function).apply(any())
      verify(tracer).stopSpan(false)
    }
    "start a timer" in {
      JMoney.startTimer("the-timer")
      verify(tracer).startTimer("the-timer")
    }
    "stop a timer" in {
      JMoney.stopTimer("the-timer")
      verify(tracer).stopTimer("the-timer")
    }
    "support closeable timer" in {
      val timer: TraceTimer = JMoney.newTimer("the-timer")
      timer.close()
      timer.close()

      verify(tracer).startTimer("the-timer")
      verify(tracer, times(1)).stopTimer("the-timer")
    }
    "support runnable timer lambda" in {
      val runnable = mock[CheckedRunnable[Exception]]

      JMoney.time("the-timer", runnable)

      verify(tracer).startTimer("the-timer")
      verify(runnable).run()
      verify(tracer).stopTimer("the-timer")
    }
    "support throwing runnable timer lambda" in {
      val runnable = mock[CheckedRunnable[Exception]]
      doThrow(new Exception()).when(runnable).run()

      assertThrows[Exception] {
        JMoney.time("the-timer", runnable)
      }

      verify(tracer).startTimer("the-timer")
      verify(runnable).run()
      verify(tracer).stopTimer("the-timer")
    }
    "support callable timer lambda" in {
      val callable = mock[CheckedCallable[String, Exception]]
      doReturn("Hello").when(callable).call()

      val result = JMoney.time("the-timer", callable)

      result shouldBe "Hello"

      verify(tracer).startTimer("the-timer")
      verify(callable).call()
      verify(tracer).stopTimer("the-timer")
    }
    "support throwing callable timer lambda" in {
      val callable = mock[CheckedCallable[String, Exception]]
      doThrow(new Exception()).when(callable).call()

      assertThrows[Exception] {
        JMoney.time("the-timer", callable)
      }

      verify(tracer).startTimer("the-timer")
      verify(callable).call()
      verify(tracer).stopTimer("the-timer")
    }
    "record long values" in {
      JMoney.record("the-long", java.lang.Long.valueOf(1L))
      verify(tracer).record("the-long", java.lang.Long.valueOf(1L), false)
    }
    "record propagate-able long values" in {
      JMoney.record("the-long", java.lang.Long.valueOf(1L), true)
      verify(tracer).record("the-long", java.lang.Long.valueOf(1L), true)
    }
    "record boolean values" in {
      JMoney.record("the-bool", true)
      verify(tracer).record("the-bool", true, false)
    }
    "record propagate-able boolean values" in {
      JMoney.record("the-bool", true, true)
      verify(tracer).record("the-bool", true, true)
    }
    "record double values" in {
      JMoney.record("the-double", java.lang.Double.valueOf(3.14))
      verify(tracer).record("the-double", java.lang.Double.valueOf(3.14), false)
    }
    "record propagate-able double values" in {
      JMoney.record("the-double", java.lang.Double.valueOf(3.14), true)
      verify(tracer).record("the-double", java.lang.Double.valueOf(3.14), true)
    }
    "record string values" in {
      JMoney.record("the-string", "yo")
      verify(tracer).record("the-string", "yo", false)
    }
    "record propagate-able string values" in {
      JMoney.record("the-string", "yo", true)
      verify(tracer).record("the-string", "yo", true)
    }
    "record a timestamp" in {
      JMoney.record("stamp-this")
      verify(tracer).time("stamp-this")
    }
    "record a long as an Object" in {
      val lng: java.lang.Long = 100L
      val obj: AnyRef = lng
      JMoney.record("long", obj)

      val captor = ArgumentCaptor.forClass(classOf[Note[_]])
      verify(tracer).record(captor.capture())
      val note = captor.getValue
      note.name shouldBe "long"
      note.value shouldBe 100L
    }
    "record a boolean as an Object" in {
      val boo: java.lang.Boolean = true
      val obj: AnyRef = boo
      JMoney.record("bool", obj)

      val captor = ArgumentCaptor.forClass(classOf[Note[_]])
      verify(tracer).record(captor.capture())
      val note = captor.getValue
      note.name shouldBe "bool"
      note.value shouldBe true
    }
    "record a double as an Object" in {
      val dbl: java.lang.Double = 3.14
      val obj: AnyRef = dbl
      JMoney.record("double", obj)

      val captor = ArgumentCaptor.forClass(classOf[Note[_]])
      verify(tracer).record(captor.capture())
      val note = captor.getValue
      note.name shouldBe "double"
      note.value shouldBe 3.14
    }
    "record a string as an Object" in {
      val str: java.lang.String = "hello"
      val obj: AnyRef = str
      JMoney.record("string", obj)

      val captor = ArgumentCaptor.forClass(classOf[Note[_]])
      verify(tracer).record(captor.capture())
      val note = captor.getValue
      note.name shouldBe "string"
      note.value shouldBe "hello"
    }
    "record any object as an Object" in {
      val lst = List(1)
      JMoney.record("list", lst)

      val captor = ArgumentCaptor.forClass(classOf[Note[_]])
      verify(tracer).record(captor.capture())
      val note = captor.getValue
      note.name shouldBe "list"
      note.value shouldBe "List(1)"
    }
    "record null as a string Note with None" in {
      val obj: AnyRef = null
      JMoney.record("nill", obj)

      val captor = ArgumentCaptor.forClass(classOf[Note[_]])
      verify(tracer).record(captor.capture())
      val note = captor.getValue
      val nil: AnyRef = null
      note.name shouldBe "nill"
      note.value shouldBe nil
    }
  }
}