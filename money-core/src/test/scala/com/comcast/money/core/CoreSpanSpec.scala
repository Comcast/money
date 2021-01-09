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
import com.comcast.money.api.{ SpanHandler, SpanId, SpanInfo }
import com.comcast.money.core.handlers.TestData
import io.opentelemetry.api.common.{ AttributeKey, Attributes }
import io.opentelemetry.context.Scope
import io.opentelemetry.api.trace.{ StatusCode, TraceFlags, TraceState }
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import InstantImplicits._
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes

class CoreSpanSpec extends AnyWordSpec with Matchers with TestData with MockitoSugar {

  "CoreSpan" should {
    "contain the start time stamp" in {
      val underTest = CoreSpan(SpanId.createNew(), "test", startTimeNanos = 1000000000)

      val info = underTest.info
      info.startTimeNanos shouldBe 1000000000
      info.startTimeMicros shouldBe 1000000
      info.startTimeMillis shouldBe 1000
    }

    "record a timer" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.startTimer("foo")
      underTest.stopTimer("foo")

      underTest.info.notes should contain key "foo"
    }

    "record a note" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.record(testLongNote)

      underTest.info.notes should contain value testLongNote
    }

    "set a String attribute" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.setAttribute("foo", "bar")

      underTest.info.notes should contain key "foo"
      val note = underTest.info.notes.get("foo")
      note.name shouldBe "foo"
      note.value shouldBe "bar"
    }

    "set a long attribute" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.setAttribute("foo", 200L)

      underTest.info.notes should contain key "foo"
      val note = underTest.info.notes.get("foo")
      note.name shouldBe "foo"
      note.value shouldBe 200L
    }

    "set a double attribute" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.setAttribute("foo", 2.2)

      underTest.info.notes should contain key "foo"
      val note = underTest.info.notes.get("foo")
      note.name shouldBe "foo"
      note.value shouldBe 2.2
    }

    "set a boolean attribute" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.setAttribute("foo", true)

      underTest.info.notes should contain key "foo"
      val note = underTest.info.notes.get("foo")
      note.name shouldBe "foo"
      note.value shouldBe true
    }

    "set a attribute with an attribute key" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.setAttribute(AttributeKey.stringKey("foo"), "bar")

      underTest.info.notes should contain key "foo"
      val note = underTest.info.notes.get("foo")
      note.name shouldBe "foo"
      note.value shouldBe "bar"
    }

    "add an event with name" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.addEvent("event")

      underTest.info.events should have size 1
      val event = underTest.info.events.get(0)
      event.name shouldBe "event"
      event.attributes should have size 0
      event.timestamp should not be 0L
      event.exception should be(null)
    }

    "add an event with name and timestamp" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.addEvent("event", 100L, TimeUnit.NANOSECONDS)

      underTest.info.events should have size 1
      val event = underTest.info.events.get(0)
      event.name shouldBe "event"
      event.attributes should have size 0
      event.timestamp shouldBe 100L
      event.exception should be(null)
    }

    "add an event with name and instant" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")
      val instant = Instant.now

      underTest.addEvent("event", instant)

      underTest.info.events should have size 1
      val event = underTest.info.events.get(0)
      event.name shouldBe "event"
      event.attributes should have size 0
      event.timestamp shouldBe instant.toEpochNano
      event.exception should be(null)
    }

    "add an event with name and attributes" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.addEvent("event", Attributes.of(AttributeKey.stringKey("foo"), "bar"))

      underTest.info.events should have size 1
      val event = underTest.info.events.get(0)
      event.name shouldBe "event"
      event.attributes should have size 1
      event.attributes.get(AttributeKey.stringKey("foo")) shouldBe "bar"
      event.exception should be(null)
    }

    "add an event with name, attributes and timestamp" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.addEvent("event", Attributes.of(AttributeKey.stringKey("foo"), "bar"), 100L, TimeUnit.NANOSECONDS)

      underTest.info.events should have size 1
      val event = underTest.info.events.get(0)
      event.name shouldBe "event"
      event.attributes should have size 1
      event.attributes.get(AttributeKey.stringKey("foo")) shouldBe "bar"
      event.timestamp shouldBe 100L
      event.exception should be(null)
    }

    "add an event with name, attributes and instant" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")
      val instant = Instant.now

      underTest.addEvent("event", Attributes.of(AttributeKey.stringKey("foo"), "bar"), instant)

      underTest.info.events should have size 1
      val event = underTest.info.events.get(0)
      event.name shouldBe "event"
      event.attributes should have size 1
      event.attributes.get(AttributeKey.stringKey("foo")) shouldBe "bar"
      event.timestamp shouldBe instant.toEpochNano
      event.exception should be(null)
    }

    "record an exception" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

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
      val underTest = CoreSpan(SpanId.createNew(), "test")

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
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.setStatus(StatusCode.OK)

      underTest.info.success shouldBe (null)

      underTest.stop()

      underTest.info.success shouldBe (true)
    }

    "set the status to ERROR" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.setStatus(StatusCode.ERROR)

      underTest.info.success shouldBe (null)

      underTest.stop()

      underTest.info.success shouldBe (false)
    }

    "set the status to OK and stop with false result" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.setStatus(StatusCode.OK)

      underTest.info.success shouldBe (null)

      underTest.stop(false)

      underTest.info.success shouldBe (false)
    }

    "set the status to ERROR and stop with true result" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.setStatus(StatusCode.ERROR)

      underTest.info.success shouldBe (null)

      underTest.stop(true)

      underTest.info.success shouldBe (true)
    }

    "set the status with description" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.setStatus(StatusCode.OK, "description")

      underTest.info.description shouldBe "description"
    }

    "update the span name" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.name shouldBe "test"
      underTest.info.name shouldBe "test"

      underTest.updateName("otherTest")

      underTest.name shouldBe "otherTest"
      underTest.info.name shouldBe "otherTest"

    }

    "gets isRecording" in {
      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.isRecording shouldBe true

      underTest.stop()

      underTest.isRecording shouldBe false
    }

    "gets SpanContext" in {
      val spanId = SpanId.createRemote("01234567-890A-BCDE-F012-34567890ABCD", 81985529216486895L, 81985529216486895L, TraceFlags.getSampled, TraceState.getDefault)
      val underTest = CoreSpan(spanId, "test")

      val context = underTest.getSpanContext

      context.getTraceIdAsHexString shouldBe "01234567890abcdef01234567890abcd"
      context.getSpanIdAsHexString shouldBe "0123456789abcdef"
    }

    "set the endTimeMillis and endTimeMicros when stopped" in {
      val clock = mock[Clock]
      when(clock.now).thenReturn(3000000000L)
      val underTest = CoreSpan(SpanId.createNew(), "test", startTimeNanos = 1000000000, clock = clock)

      underTest.stop(true)

      val state = underTest.info

      state.endTimeNanos shouldBe 3000000000L
      state.endTimeMicros shouldBe 3000000L
      state.endTimeMillis shouldBe 3000L
      state.durationNanos shouldBe 2000000000L
      state.durationMicros shouldBe 2000000L
    }

    "invoke the span handler when stopped" in {
      val handler = mock[SpanHandler]
      val handleCaptor = ArgumentCaptor.forClass(classOf[SpanInfo])
      val underTest = CoreSpan(SpanId.createNew(), "test", handler = handler, startTimeNanos = SystemClock.now)

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
      val underTest = CoreSpan(SpanId.createNew(), "test", handler = handler, startTimeNanos = SystemClock.now)

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

      val underTest = CoreSpan(SpanId.createNew(), "test")

      underTest.attachScope(scope1)
      underTest.attachScope(scope2)

      underTest.stop()

      verify(scope1).close()
      verify(scope2).close()
    }

    "invoke the span handler when ended" in {
      val handler = mock[SpanHandler]
      val handleCaptor = ArgumentCaptor.forClass(classOf[SpanInfo])
      val underTest = CoreSpan(SpanId.createNew(), "test", handler = handler, startTimeNanos = SystemClock.now)

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

    "invoke the span handler when ended with timestamp" in {
      val handler = mock[SpanHandler]
      val handleCaptor = ArgumentCaptor.forClass(classOf[SpanInfo])
      val underTest = CoreSpan(SpanId.createNew(), "test", handler = handler, startTimeNanos = SystemClock.now)

      underTest.record(testLongNote)
      underTest.end(1L, TimeUnit.MILLISECONDS)

      verify(handler).handle(handleCaptor.capture())

      val handledInfo = handleCaptor.getValue

      handledInfo.id shouldBe underTest.id
      handledInfo.startTimeMicros.toInt should not be 0
      handledInfo.startTimeMillis.toInt should not be 0
      handledInfo.endTimeMicros.toInt shouldBe 1000
      handledInfo.endTimeMillis.toInt shouldBe 1
      handledInfo.notes should contain value testLongNote
    }

    "invoke the span handler when ended with instant" in {
      val handler = mock[SpanHandler]
      val handleCaptor = ArgumentCaptor.forClass(classOf[SpanInfo])
      val underTest = CoreSpan(SpanId.createNew(), "test", handler = handler, startTimeNanos = SystemClock.now)
      val instant = Instant.now

      underTest.record(testLongNote)
      underTest.end(instant)

      verify(handler).handle(handleCaptor.capture())

      val handledInfo = handleCaptor.getValue

      handledInfo.id shouldBe underTest.id
      handledInfo.startTimeNanos should not be 0
      handledInfo.endTimeNanos shouldBe instant.toEpochNano
      handledInfo.notes should contain value testLongNote
    }
  }
}
