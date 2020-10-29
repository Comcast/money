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

import com.comcast.money.api.{ InstrumentationLibrary, Note, Span, SpanFactory, SpanHandler, SpanId, SpanInfo }
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
import scala.collection.JavaConverters._

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

    "create a span with a specific id" in {
      val handler = mock[SpanHandler]
      val sampler = mock[Sampler]

      val spanId = SpanId.createNew()
      val span = mock[Span]
      when(sampler.shouldSample(argEq(spanId), argEq(None), argEq("test"))).thenReturn(SamplerResult.RecordAndSample)

      val underTest = new CoreSpanBuilder(Some(spanId), None, "test", clock, handler, sampler, library) {
        override private[core] def createSpan(id: SpanId, name: String, kind: OtelSpan.Kind) = {
          id shouldBe spanId
          name shouldBe "test"
          span
        }
      }

      val result = underTest.startSpan()
      result shouldBe span

      verify(span).start()
    }

    "create a child span" in {
      val handler = mock[SpanHandler]
      val sampler = mock[Sampler]

      val parentSpanId = SpanId.createNew()
      val parentSpanInfo = mock[SpanInfo]
      val parentSpan = mock[Span]
      val span = mock[Span]
      when(parentSpan.info).thenReturn(parentSpanInfo)
      when(parentSpanInfo.id).thenReturn(parentSpanId)
      when(sampler.shouldSample(any(), argEq(Some(parentSpanId)), argEq("test"))).thenReturn(SamplerResult.RecordAndSample)

      val underTest = new CoreSpanBuilder(None, Some(parentSpan), "test", clock, handler, sampler, library) {
        override private[core] def createSpan(id: SpanId, name: String, kind: OtelSpan.Kind) = {
          id.traceId shouldBe parentSpanId.traceId
          id.parentId shouldBe parentSpanId.selfId
          name shouldBe "test"
          span
        }
      }

      val result = underTest.startSpan()
      result shouldBe span

      verify(span).start()
    }

    "create a child span with an explicit parent span" in {
      val handler = mock[SpanHandler]
      val sampler = mock[Sampler]

      val parentSpanId = SpanId.createNew()
      val parentSpanInfo = mock[SpanInfo]
      val parentSpan = mock[Span]
      val span = mock[Span]
      when(parentSpan.info).thenReturn(parentSpanInfo)
      when(parentSpanInfo.id).thenReturn(parentSpanId)
      when(sampler.shouldSample(any(), argEq(Some(parentSpanId)), argEq("test"))).thenReturn(SamplerResult.RecordAndSample)

      val underTest = new CoreSpanBuilder(None, None, "test", clock, handler, sampler, library) {
        override private[core] def createSpan(id: SpanId, name: String, kind: OtelSpan.Kind) = {
          id.traceId shouldBe parentSpanId.traceId
          id.parentId shouldBe parentSpanId.selfId
          name shouldBe "test"
          span
        }
      }

      val result = underTest
        .setParent(parentSpan)
        .startSpan()
      result shouldBe span

      verify(span).start()
    }

    "create a child span with an explicit parent span wrapped in an Option" in {
      val handler = mock[SpanHandler]
      val sampler = mock[Sampler]

      val parentSpanId = SpanId.createNew()
      val parentSpanInfo = mock[SpanInfo]
      val parentSpan = mock[Span]
      val span = mock[Span]
      when(parentSpan.info).thenReturn(parentSpanInfo)
      when(parentSpanInfo.id).thenReturn(parentSpanId)
      when(sampler.shouldSample(any(), argEq(Some(parentSpanId)), argEq("test"))).thenReturn(SamplerResult.RecordAndSample)

      val underTest = new CoreSpanBuilder(None, None, "test", clock, handler, sampler, library) {
        override private[core] def createSpan(id: SpanId, name: String, kind: OtelSpan.Kind) = {
          id.traceId shouldBe parentSpanId.traceId
          id.parentId shouldBe parentSpanId.selfId
          name shouldBe "test"
          span
        }
      }

      val result = underTest
        .setParent(Some(parentSpan))
        .startSpan()
      result shouldBe span

      verify(span).start()
    }

    "create a child span with an explicit parent span wrapped in an Context" in {
      val handler = mock[SpanHandler]
      val sampler = mock[Sampler]

      val parentSpanId = SpanId.createNew()
      val parentSpanInfo = mock[SpanInfo]
      val parentSpan = mock[Span]
      val span = mock[Span]
      when(parentSpan.info).thenReturn(parentSpanInfo)
      when(parentSpanInfo.id).thenReturn(parentSpanId)
      when(sampler.shouldSample(any(), argEq(Some(parentSpanId)), argEq("test"))).thenReturn(SamplerResult.RecordAndSample)

      val underTest = new CoreSpanBuilder(None, None, "test", clock, handler, sampler, library) {
        override private[core] def createSpan(id: SpanId, name: String, kind: OtelSpan.Kind) = {
          id.traceId shouldBe parentSpanId.traceId
          id.parentId shouldBe parentSpanId.selfId
          name shouldBe "test"
          span
        }
      }

      val context = TracingContextUtils.withSpan(parentSpan, Context.ROOT)

      val result = underTest
        .setParent(context)
        .startSpan()
      result shouldBe span

      verify(span).start()
    }

    "create a span explicitly without a parent span" in {
      val handler = mock[SpanHandler]
      val sampler = mock[Sampler]
      val span = mock[Span]
      val parentSpan = mock[Span]
      when(sampler.shouldSample(any(), argEq(None), argEq("test"))).thenReturn(SamplerResult.RecordAndSample)

      val underTest = new CoreSpanBuilder(None, Some(parentSpan), "test", clock, handler, sampler, library) {
        override private[core] def createSpan(id: SpanId, name: String, kind: OtelSpan.Kind) = {
          name shouldBe "test"
          span
        }
      }

      val result = underTest
        .setNoParent()
        .startSpan()
      result shouldBe span

      verify(span).start()
    }

    "create a span with notes" in {
      val handler = mock[SpanHandler]
      val sampler = mock[Sampler]
      val span = mock[Span]
      when(sampler.shouldSample(any(), argEq(None), argEq("test"))).thenReturn(SamplerResult.RecordAndSample)

      val underTest = new CoreSpanBuilder(None, None, "test", clock, handler, sampler, library) {
        override private[core] def createSpan(id: SpanId, name: String, kind: OtelSpan.Kind) = span
      }

      val result = underTest
        .setAttribute("stringKey", "string")
        .setAttribute("longKey", 123L)
        .setAttribute("doubleKey", 2.2)
        .setAttribute("booleanKey", true)
        .setAttribute(AttributeKey.stringKey("attributeKey"), "string")
        .record(Note.of("note", "string"))
        .startSpan()

      result shouldBe span
      val captor: ArgumentCaptor[Note[_]] = ArgumentCaptor.forClass(classOf[Note[_]])
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

    "create a child span propagating parent span notes" in {
      val handler = mock[SpanHandler]
      val sampler = mock[Sampler]

      val parentSpanId = SpanId.createNew()
      val parentSpanInfo = mock[SpanInfo]
      val parentSpan = mock[Span]
      val span = mock[Span]
      when(parentSpan.info).thenReturn(parentSpanInfo)
      when(parentSpanInfo.id).thenReturn(parentSpanId)
      val notes: Map[String, Note[_]] = Map(
        "test" -> Note.of("some", "note", true),
        "other" -> Note.of("other", "note", false))
      when(parentSpanInfo.notes).thenReturn(notes.asJava)
      when(sampler.shouldSample(any(), argEq(Some(parentSpanId)), argEq("test"))).thenReturn(SamplerResult.RecordAndSample)

      val underTest = new CoreSpanBuilder(None, Some(parentSpan), "test", clock, handler, sampler, library) {
        override private[core] def createSpan(id: SpanId, name: String, kind: OtelSpan.Kind) = span
      }

      val result = underTest
        .setSticky(true)
        .startSpan()
      result shouldBe span

      verify(span).start()
      val captor: ArgumentCaptor[Note[_]] = ArgumentCaptor.forClass(classOf[Note[_]])
      verify(span, times(1)).record(captor.capture())
      val note = captor.getValue
      note.name shouldBe "some"
      note.value shouldBe "note"
    }

    "create a span without propagating parent span notes" in {
      val handler = mock[SpanHandler]
      val sampler = mock[Sampler]

      val parentSpanId = SpanId.createNew()
      val parentSpanInfo = mock[SpanInfo]
      val parentSpan = mock[Span]
      val span = mock[Span]
      when(parentSpan.info).thenReturn(parentSpanInfo)
      when(parentSpanInfo.id).thenReturn(parentSpanId)
      val notes: Map[String, Note[_]] = Map(
        "test" -> Note.of("some", "note", true),
        "other" -> Note.of("other", "note", false))
      when(parentSpanInfo.notes).thenReturn(notes.asJava)
      when(sampler.shouldSample(any(), argEq(Some(parentSpanId)), argEq("test"))).thenReturn(SamplerResult.RecordAndSample)

      val underTest = new CoreSpanBuilder(None, Some(parentSpan), "test", clock, handler, sampler, library) {
        override private[core] def createSpan(id: SpanId, name: String, kind: OtelSpan.Kind) = span
      }

      val result = underTest
        .setSticky(false)
        .startSpan()
      result shouldBe span

      verify(span).start()
      verify(span, never).record(any())
    }

    "create a span with a specific kind" in {
      val handler = mock[SpanHandler]
      val sampler = mock[Sampler]
      val span = mock[Span]
      when(sampler.shouldSample(any(), argEq(None), argEq("test"))).thenReturn(SamplerResult.RecordAndSample)

      val underTest = new CoreSpanBuilder(None, None, "test", clock, handler, sampler, library)

      val result = underTest
        .setSpanKind(OtelSpan.Kind.SERVER)
        .startSpan()

      result.info.kind shouldBe OtelSpan.Kind.SERVER
    }

    "create an unrecorded span" in {
      val handler = mock[SpanHandler]
      val sampler = mock[Sampler]
      when(sampler.shouldSample(any(), argEq(None), argEq("test"))).thenReturn(SamplerResult.Drop)

      val underTest = new CoreSpanBuilder(None, None, "test", clock, handler, sampler, library)

      val result = underTest
        .setSpanKind(OtelSpan.Kind.SERVER)
        .startSpan()

      result shouldBe a[UnrecordedSpan]
    }

    "create a sampled span" in {
      val handler = mock[SpanHandler]
      val sampler = mock[Sampler]
      val span = mock[Span]
      when(sampler.shouldSample(any(), argEq(None), argEq("test"))).thenReturn(SamplerResult.RecordAndSample)

      val underTest = new CoreSpanBuilder(None, None, "test", clock, handler, sampler, library)

      val result = underTest.startSpan()

      result.info.id.isSampled shouldBe true
    }

    "create a recorded span" in {
      val handler = mock[SpanHandler]
      val sampler = mock[Sampler]
      val span = mock[Span]
      when(sampler.shouldSample(any(), argEq(None), argEq("test"))).thenReturn(SamplerResult.Record)

      val underTest = new CoreSpanBuilder(None, None, "test", clock, handler, sampler, library)

      val result = underTest.startSpan()

      result.info.id.isSampled shouldBe false
    }

    "create a span with sampler notes" in {
      val handler = mock[SpanHandler]
      val sampler = mock[Sampler]

      val span = mock[Span]
      when(sampler.shouldSample(any(), argEq(None), argEq("test")))
        .thenReturn(SamplerResult.RecordAndSample.withNote(Note.of("sampler", "note")))

      val underTest = new CoreSpanBuilder(None, None, "test", clock, handler, sampler, library) {
        override private[core] def createSpan(id: SpanId, name: String, kind: OtelSpan.Kind) = span
      }

      val result = underTest.startSpan()
      result shouldBe span

      verify(span).start()
      val captor: ArgumentCaptor[Note[_]] = ArgumentCaptor.forClass(classOf[Note[_]])
      verify(span, times(1)).record(captor.capture())
      val note = captor.getValue
      note.name shouldBe "sampler"
      note.value shouldBe "note"
    }

    "create a span with an explicit start timestamp" in {
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

      val result = underTest
        .setStartTimestamp(1000000002)
        .startSpan()
      result shouldBe span

      verify(span).start(1, 2)
    }

    "builds a span without starting it" in {
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

      val result = underTest.build()
      result shouldBe span

      verify(span, never).start()
      verify(span, never).start(anyLong, anyInt)
    }

    /*

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
