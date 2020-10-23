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

import java.time.Instant
import java.util.concurrent.TimeUnit

import com.comcast.money.api.{ InstrumentationLibrary, SpanHandler, SpanId, SpanInfo }
import com.comcast.money.core.handlers.TestData
import io.opentelemetry.common.{ AttributeKey, Attributes }
import io.opentelemetry.context.Scope
import io.opentelemetry.trace.attributes.SemanticAttributes
import io.opentelemetry.trace.{ EndSpanOptions, StatusCanonicalCode, TraceFlags, TraceState, Span => OtelSpan }
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class CoreSpanSpec extends AnyWordSpec with Matchers with TestData with MockitoSugar {

  "CoreSpan" should {
    "set the startTimeMillis and startTimeMicros when started" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)
      underTest.start()

      val state = underTest.info

      state.startTimeMicros.toInt should not be 0
      state.startTimeMillis.toInt should not be 0
    }

    "set the startTimeMillis and startTimeMicros when started with time stamps" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)
      val instant = Instant.now
      underTest.start(instant.getEpochSecond, instant.getNano)

      val state = underTest.info

      state.startTimeMicros shouldBe TimeUnit.SECONDS.toMicros(instant.getEpochSecond) + TimeUnit.NANOSECONDS.toMicros(instant.getNano)
      state.startTimeMillis shouldBe instant.toEpochMilli
    }

    "record a timer" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

      underTest.startTimer("foo")
      underTest.stopTimer("foo")

      underTest.info.notes should contain key "foo"
    }

    "record a note" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

      underTest.record(testLongNote)

      underTest.info.notes should contain value testLongNote
    }

    "set a String attribute" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

      underTest.setAttribute("foo", "bar")

      underTest.info.notes should contain key "foo"
      val note = underTest.info.notes.get("foo")
      note.name shouldBe "foo"
      note.value shouldBe "bar"
    }

    "set a long attribute" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

      underTest.setAttribute("foo", 200L)

      underTest.info.notes should contain key "foo"
      val note = underTest.info.notes.get("foo")
      note.name shouldBe "foo"
      note.value shouldBe 200L
    }

    "set a double attribute" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

      underTest.setAttribute("foo", 2.2)

      underTest.info.notes should contain key "foo"
      val note = underTest.info.notes.get("foo")
      note.name shouldBe "foo"
      note.value shouldBe 2.2
    }

    "set a boolean attribute" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

      underTest.setAttribute("foo", true)

      underTest.info.notes should contain key "foo"
      val note = underTest.info.notes.get("foo")
      note.name shouldBe "foo"
      note.value shouldBe true
    }

    "set a attribute with an attribute key" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

      underTest.setAttribute(AttributeKey.stringKey("foo"), "bar")

      underTest.info.notes should contain key "foo"
      val note = underTest.info.notes.get("foo")
      note.name shouldBe "foo"
      note.value shouldBe "bar"
    }

    "add an event with name" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

      underTest.addEvent("event")

      underTest.info.events should have size 1
      val event = underTest.info.events.get(0)
      event.name shouldBe "event"
      event.attributes should have size 0
      event.timestamp should not be 0L
      event.exception should be(null)
    }

    "add an event with name and timestamp" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

      underTest.addEvent("event", 100L)

      underTest.info.events should have size 1
      val event = underTest.info.events.get(0)
      event.name shouldBe "event"
      event.attributes should have size 0
      event.timestamp shouldBe 100L
      event.exception should be(null)
    }

    "add an event with name and attributes" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

      underTest.addEvent("event", Attributes.of(AttributeKey.stringKey("foo"), "bar"))

      underTest.info.events should have size 1
      val event = underTest.info.events.get(0)
      event.name shouldBe "event"
      event.attributes should have size 1
      event.attributes.get(AttributeKey.stringKey("foo")) shouldBe "bar"
      event.exception should be(null)
    }

    "add an event with name, attributes and timestamp" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

      underTest.addEvent("event", Attributes.of(AttributeKey.stringKey("foo"), "bar"), 100L)

      underTest.info.events should have size 1
      val event = underTest.info.events.get(0)
      event.name shouldBe "event"
      event.attributes should have size 1
      event.attributes.get(AttributeKey.stringKey("foo")) shouldBe "bar"
      event.timestamp shouldBe 100L
      event.exception should be(null)
    }

    "record an exception" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

      val exception = new RuntimeException("BOOM")
      underTest.recordException(exception)

      underTest.info.events should have size 1
      val event = underTest.info.events.get(0)
      event.name shouldBe SemanticAttributes.EXCEPTION_EVENT_NAME
      event.attributes should have size 3
      event.attributes.get(SemanticAttributes.EXCEPTION_TYPE) shouldBe "java.lang.RuntimeException"
      event.attributes.get(SemanticAttributes.EXCEPTION_MESSAGE) shouldBe "BOOM"
      event.attributes.get(SemanticAttributes.EXCEPTION_STACKTRACE) should startWith("java.lang.RuntimeException: BOOM")
      event.timestamp should not be 0
      event.exception shouldBe exception
    }

    "record an exception with attributes" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

      val exception = new RuntimeException("BOOM")
      underTest.recordException(exception, Attributes.of(AttributeKey.stringKey("foo"), "bar"))

      underTest.info.events should have size 1
      val event = underTest.info.events.get(0)
      event.name shouldBe SemanticAttributes.EXCEPTION_EVENT_NAME
      event.attributes should have size 4
      event.attributes.get(SemanticAttributes.EXCEPTION_TYPE) shouldBe "java.lang.RuntimeException"
      event.attributes.get(SemanticAttributes.EXCEPTION_MESSAGE) shouldBe "BOOM"
      event.attributes.get(SemanticAttributes.EXCEPTION_STACKTRACE) should startWith("java.lang.RuntimeException: BOOM")
      event.attributes.get(AttributeKey.stringKey("foo")) shouldBe "bar"
      event.timestamp should not be 0
      event.exception shouldBe exception
    }

    "set the status to OK" in {
      val handler = mock[SpanHandler]
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, handler)

      underTest.setStatus(StatusCanonicalCode.OK)

      underTest.info.success shouldBe (null)

      underTest.stop()

      underTest.info.success shouldBe (true)
    }

    "set the status to ERROR" in {
      val handler = mock[SpanHandler]
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, handler)

      underTest.setStatus(StatusCanonicalCode.ERROR)

      underTest.info.success shouldBe (null)

      underTest.stop()

      underTest.info.success shouldBe (false)
    }

    "set the status to OK and stop with false result" in {
      val handler = mock[SpanHandler]
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, handler)

      underTest.setStatus(StatusCanonicalCode.OK)

      underTest.info.success shouldBe (null)

      underTest.stop(false)

      underTest.info.success shouldBe (false)
    }

    "set the status to ERROR and stop with true result" in {
      val handler = mock[SpanHandler]
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, handler)

      underTest.setStatus(StatusCanonicalCode.ERROR)

      underTest.info.success shouldBe (null)

      underTest.stop(true)

      underTest.info.success shouldBe (true)
    }

    "set the status with description" in {
      val handler = mock[SpanHandler]
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, handler)

      underTest.setStatus(StatusCanonicalCode.OK, "description")

      underTest.info.description shouldBe "description"
    }

    "update the span name" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

      underTest.name shouldBe "test"
      underTest.info.name shouldBe "test"

      underTest.updateName("otherTest")

      underTest.name shouldBe "otherTest"
      underTest.info.name shouldBe "otherTest"

    }

    "gets isRecording" in {
      val handler = mock[SpanHandler]
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, handler)

      underTest.isRecording shouldBe false

      underTest.start()

      underTest.isRecording shouldBe true

      underTest.stop()

      underTest.isRecording shouldBe false
    }

    "gets SpanContext" in {
      val spanId = SpanId.createRemote("01234567-890A-BCDE-F012-34567890ABCD", 81985529216486895L, 81985529216486895L, TraceFlags.getSampled, TraceState.getDefault)
      val underTest = CoreSpan(spanId, "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

      val context = underTest.getContext

      context.getTraceIdAsHexString shouldBe "01234567890abcdef01234567890abcd"
      context.getSpanIdAsHexString shouldBe "0123456789abcdef"
    }

    "set the endTimeMillis and endTimeMicros when stopped" in {
      val handler = mock[SpanHandler]
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, handler)

      underTest.stop(true)

      val state = underTest.info

      state.endTimeMicros.toInt should not be 0
      state.endTimeMillis.toInt should not be 0
    }

    "invoke the span handler when stopped" in {
      val handler = mock[SpanHandler]
      val handleCaptor = ArgumentCaptor.forClass(classOf[SpanInfo])
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, handler)

      underTest.start()
      underTest.record(testLongNote)
      underTest.stop(true)

      verify(handler).handle(handleCaptor.capture())

      val handledInfo = handleCaptor.getValue

      handledInfo.id shouldBe underTest.id
      handledInfo.startTimeMicros.toInt should not be 0
      handledInfo.startTimeMillis.toInt should not be 0
      handledInfo.endTimeMicros.toInt should not be 0
      handledInfo.endTimeMillis.toInt should not be 0
      handledInfo.notes should contain value testLongNote
    }

    "invoke the span handler when closed" in {
      val handler = mock[SpanHandler]
      val handleCaptor = ArgumentCaptor.forClass(classOf[SpanInfo])
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, handler)

      underTest.start()
      underTest.record(testLongNote)
      underTest.close()

      verify(handler).handle(handleCaptor.capture())

      val handledInfo = handleCaptor.getValue

      handledInfo.id shouldBe underTest.id
      handledInfo.startTimeMicros.toInt should not be 0
      handledInfo.startTimeMillis.toInt should not be 0
      handledInfo.endTimeMicros.toInt should not be 0
      handledInfo.endTimeMillis.toInt should not be 0
      handledInfo.notes should contain value testLongNote
    }

    "closes attached scopes" in {
      val scope1 = mock[Scope]
      val scope2 = mock[Scope]

      val handler = mock[SpanHandler]
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, handler)

      underTest.attachScope(scope1)
      underTest.attachScope(scope2)

      underTest.stop()

      verify(scope1).close()
      verify(scope2).close()
    }

    "invoke the span handler when ended" in {
      val handler = mock[SpanHandler]
      val handleCaptor = ArgumentCaptor.forClass(classOf[SpanInfo])
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, handler)

      underTest.start()
      underTest.record(testLongNote)
      underTest.end()

      verify(handler).handle(handleCaptor.capture())

      val handledInfo = handleCaptor.getValue

      handledInfo.id shouldBe underTest.id
      handledInfo.startTimeMicros.toInt should not be 0
      handledInfo.startTimeMillis.toInt should not be 0
      handledInfo.endTimeMicros.toInt should not be 0
      handledInfo.endTimeMillis.toInt should not be 0
      handledInfo.notes should contain value testLongNote
    }

    "invoke the span handler when ended with options" in {
      val handler = mock[SpanHandler]
      val handleCaptor = ArgumentCaptor.forClass(classOf[SpanInfo])
      val underTest = CoreSpan(SpanId.createNew(), "test", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, handler)

      underTest.start()
      underTest.record(testLongNote)
      underTest.end(EndSpanOptions
        .builder()
        .setEndTimestamp(TimeUnit.MILLISECONDS.toNanos(1L))
        .build())

      verify(handler).handle(handleCaptor.capture())

      val handledInfo = handleCaptor.getValue

      handledInfo.id shouldBe underTest.id
      handledInfo.startTimeMicros.toInt should not be 0
      handledInfo.startTimeMillis.toInt should not be 0
      handledInfo.endTimeMicros.toInt shouldBe 1000
      handledInfo.endTimeMillis.toInt shouldBe 1
      handledInfo.notes should contain value testLongNote
    }
  }
}
