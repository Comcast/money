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

package com.comcast.money.core

import com.comcast.money.api.{ InstrumentationLibrary, Note, Span, SpanFactory, SpanHandler, SpanId }
import com.comcast.money.core.samplers.{ Sampler, SamplerResult }
import io.grpc.Context
import io.opentelemetry.common.AttributeKey
import io.opentelemetry.trace.{ TracingContextUtils, Span => OtelSpan }
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{ any, anyInt, anyLong, eq => argEq }
import org.mockito.Mockito.{ never, times, verify, when }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class CoreSpanBuilderSpec extends AnyWordSpec with Matchers with MockitoSugar {
  val clock = SystemClock
  val library = InstrumentationLibrary.UNKNOWN

  "CoreSpanBuilder" should {
    "create a span" in {
      val handler = mock[SpanHandler]
      val sampler = mock[Sampler]
      val span = mock[Span]
      when(sampler.shouldSample(any(), argEq(None), argEq("test"))).thenReturn(SamplerResult.RecordAndSample)

      val underTest = new CoreSpanBuilder(None, None, "test", clock, handler, sampler, library) {
        override private[core] def createSpan(id: SpanId, name: String, kind: OtelSpan.Kind) = {
          name shouldBe "test"
          span
        }
      }

      val result = underTest.startSpan()
      result shouldBe span

      verify(span).start()
    }

    /*
    "create a span with a parent span" in {
      val spanFactory = mock[SpanFactory]
      val parentSpan = mock[Span]
      val span = mock[Span]
      when(spanFactory.childSpan("test", parentSpan, true)).thenReturn(span)

      val underTest = new CoreSpanBuilder(Some(parentSpan), "test", spanFactory)

      val result = underTest.startSpan()
      result shouldBe span

      verify(spanFactory).childSpan("test", parentSpan, true)
      verify(span).start()
    }

    "create a span with an explicit parent span" in {
      val spanFactory = mock[SpanFactory]
      val parentSpan = mock[Span]
      val span = mock[Span]
      when(spanFactory.childSpan("test", parentSpan, true)).thenReturn(span)

      val underTest = new CoreSpanBuilder(None, "test", spanFactory)
        .setParent(parentSpan)

      val result = underTest.startSpan()
      result shouldBe span

      verify(spanFactory).childSpan("test", parentSpan, true)
      verify(span).start()
    }

    "create a span with an explicit parent span wrapped in Option" in {
      val spanFactory = mock[SpanFactory]
      val parentSpan = mock[Span]
      val span = mock[Span]
      when(spanFactory.childSpan("test", parentSpan, true)).thenReturn(span)

      val underTest = new CoreSpanBuilder(None, "test", spanFactory)
        .setParent(Some(parentSpan))

      val result = underTest.startSpan()
      result shouldBe span

      verify(spanFactory).childSpan("test", parentSpan, true)
      verify(span).start()
    }

    "create a span with an parent span from context" in {
      val spanFactory = mock[SpanFactory]
      val parentSpan = mock[Span]
      val updatedContext = TracingContextUtils.withSpan(parentSpan, Context.current())
      val span = mock[Span]
      when(spanFactory.childSpan("test", parentSpan, true)).thenReturn(span)

      val underTest = new CoreSpanBuilder(None, "test", spanFactory)
        .setParent(updatedContext)

      val result = underTest.startSpan()
      result shouldBe span

      verify(spanFactory).childSpan("test", parentSpan, true)
      verify(span).start()
    }

    "create a span with a parent span without propagating notes" in {
      val spanFactory = mock[SpanFactory]
      val parentSpan = mock[Span]
      val span = mock[Span]
      when(spanFactory.childSpan("test", parentSpan, false)).thenReturn(span)

      val underTest = new CoreSpanBuilder(Some(parentSpan), "test", spanFactory)
        .setSticky(false)

      val result = underTest.startSpan()
      result shouldBe span

      verify(spanFactory).childSpan("test", parentSpan, false)
      verify(span).start()
    }

    "create a span without an explicit parent span" in {
      val spanFactory = mock[SpanFactory]
      val parentSpan = mock[Span]
      val span = mock[Span]
      when(spanFactory.newSpan("test")).thenReturn(span)

      val underTest = new CoreSpanBuilder(Some(parentSpan), "test", spanFactory)
        .setNoParent()

      val result = underTest.startSpan()
      result shouldBe span

      verify(spanFactory).newSpan("test")
      verify(span).start()
    }

    "create a span with notes" in {
      val spanFactory = mock[SpanFactory]
      val span = mock[Span]
      when(spanFactory.newSpan("test")).thenReturn(span)

      val underTest = new CoreSpanBuilder(None, "test", spanFactory)
        .setAttribute("stringKey", "string")
        .setAttribute("longKey", 123L)
        .setAttribute("doubleKey", 2.2)
        .setAttribute("booleanKey", true)
        .setAttribute(AttributeKey.stringKey("attributeKey"), "string")
        .record(Note.of("note", "string"))

      val result = underTest.startSpan()
      result shouldBe span
      verify(spanFactory).newSpan("test")
      val captor = ArgumentCaptor.forClass(classOf[Note[_]])
      verify(span, times(6)).record(captor.capture())
      val notes = captor.getAllValues
      notes.get(0).name shouldBe "note"
      notes.get(0).value shouldBe "string"
      notes.get(1).name shouldBe "attributeKey"
      notes.get(1).value shouldBe "string"
      notes.get(2).name shouldBe "booleanKey"
      notes.get(2).value shouldBe true
      notes.get(3).name shouldBe "doubleKey"
      notes.get(3).value shouldBe 2.2
      notes.get(4).name shouldBe "longKey"
      notes.get(4).value shouldBe 123L
      notes.get(5).name shouldBe "stringKey"
      notes.get(5).value shouldBe "string"
      verify(span).start()
    }

    "create a span with a specific kind" in {
      val spanFactory = mock[SpanFactory]
      val span = mock[Span]
      when(spanFactory.newSpan("test")).thenReturn(span)

      val underTest = new CoreSpanBuilder(None, "test", spanFactory)
        .setSpanKind(OtelSpan.Kind.SERVER)

      val result = underTest.startSpan()
      result shouldBe span
      //TODO: fix test after wiring up Kind in CoreSpan
      // result.info.kind shouldBe OtelSpan.Kind.SERVER

      verify(spanFactory).newSpan("test")
      verify(span).start()
    }

    "create a span with an explicit start time" in {
      val spanFactory = mock[SpanFactory]
      val span = mock[Span]
      when(spanFactory.newSpan("test")).thenReturn(span)

      val underTest = new CoreSpanBuilder(None, "test", spanFactory)
        .setStartTimestamp(1000000002)

      val result = underTest.startSpan()
      result shouldBe span

      verify(spanFactory).newSpan("test")
      verify(span).start(1, 2)
    }

    "builds a span without starting" in {
      val spanFactory = mock[SpanFactory]
      val span = mock[Span]
      when(spanFactory.newSpan("test")).thenReturn(span)

      val underTest = new CoreSpanBuilder(None, "test", spanFactory)

      val result = underTest.build()
      result shouldBe span

      verify(spanFactory).newSpan("test")
      verify(span, never).start()
      verify(span, never).start(anyLong(), anyInt())
    }
     */
  }
}
