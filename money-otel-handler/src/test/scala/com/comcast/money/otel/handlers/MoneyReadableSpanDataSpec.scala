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

package com.comcast.money.otel.handlers

import java.util
import java.util.UUID

import com.comcast.money.api.{ Event, InstrumentationLibrary, Note, SpanId, SpanInfo }
import io.opentelemetry.common.{ AttributeKey, Attributes }
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.data.ImmutableStatus
import io.opentelemetry.trace.{ Span, StatusCanonicalCode, TraceState }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.JavaConverters._

class MoneyReadableSpanDataSpec extends AnyWordSpec with Matchers {
  val spanId = SpanId.createFrom(UUID.fromString("01234567-890A-BCDE-F012-34567890ABCD"), 81985529216486895L, 81985529216486895L)
  val childSpanId = SpanId.createFrom(UUID.fromString("01234567-890A-BCDE-F012-34567890ABCD"), 1147797409030816545L, 81985529216486895L)

  "MoneyReadableSpanDataSpec" should {
    "wrap Money SpanInfo" in {
      val underTest = new MoneyReadableSpanData(TestSpanInfo(spanId))

      underTest.getInstrumentationLibraryInfo.getName shouldBe "money"
      underTest.getTraceId shouldBe "01234567890abcdef01234567890abcd"
      underTest.getSpanId shouldBe "0123456789abcdef"
      underTest.getParentSpanId shouldBe "0000000000000000"
      underTest.getName shouldBe "name"
      underTest.getKind shouldBe Span.Kind.INTERNAL
      underTest.isSampled shouldBe true
      underTest.getTraceState shouldBe TraceState.getDefault
      underTest.getStartEpochNanos shouldBe 1000000L
      underTest.getEndEpochNanos shouldBe 3000000L
      underTest.hasEnded shouldBe true
      underTest.getHasEnded shouldBe true
      underTest.getLinks shouldBe empty
      underTest.getTotalRecordedLinks shouldBe 0
      underTest.getResource shouldBe Resource.getDefault
      underTest.getHasRemoteParent shouldBe false
      underTest.getLatencyNanos shouldBe 2000000L
      underTest.getStatus shouldBe ImmutableStatus.create(StatusCanonicalCode.OK, "description")
      underTest.getTotalAttributeCount shouldBe 1
      underTest.getAttributes shouldBe Attributes.of(AttributeKey.stringKey("foo"), "bar")
      underTest.getTotalRecordedEvents shouldBe 1
      underTest.getEvents.asScala should contain(MoneyEvent(event))
      underTest.toSpanData shouldBe underTest
    }

    "wrap child Money SpanInfo" in {
      val underTest = new MoneyReadableSpanData(TestSpanInfo(childSpanId))

      underTest.getInstrumentationLibraryInfo.getName shouldBe "money"
      underTest.getTraceId shouldBe "01234567890abcdef01234567890abcd"
      underTest.getSpanId shouldBe "0123456789abcdef"
      underTest.getParentSpanId shouldBe "0fedcba987654321"
      underTest.getName shouldBe "name"
      underTest.getKind shouldBe Span.Kind.INTERNAL
      underTest.isSampled shouldBe true
      underTest.getTraceState shouldBe TraceState.getDefault
      underTest.getStartEpochNanos shouldBe 1000000L
      underTest.getEndEpochNanos shouldBe 3000000L
      underTest.hasEnded shouldBe true
      underTest.getHasEnded shouldBe true
      underTest.getLinks shouldBe empty
      underTest.getTotalRecordedLinks shouldBe 0
      underTest.getResource shouldBe Resource.getDefault
      underTest.getHasRemoteParent shouldBe false
      underTest.getLatencyNanos shouldBe 2000000L
      underTest.getStatus shouldBe ImmutableStatus.create(StatusCanonicalCode.OK, "description")
      underTest.getTotalAttributeCount shouldBe 1
      underTest.getAttributes shouldBe Attributes.of(AttributeKey.stringKey("foo"), "bar")
      underTest.getTotalRecordedEvents shouldBe 1
      underTest.getEvents.asScala should contain(MoneyEvent(event))
      underTest.toSpanData shouldBe underTest
    }
  }

  case class TestSpanInfo(id: SpanId) extends SpanInfo {
    override def appName(): String = "app"
    override def host(): String = "host"
    override def library(): InstrumentationLibrary = new InstrumentationLibrary("test", "0.0.1")
    override def name(): String = "name"
    override def kind(): Span.Kind = Span.Kind.INTERNAL
    override def startTimeNanos(): Long = 1000000L
    override def endTimeNanos(): Long = 3000000L
    override def status(): StatusCanonicalCode = StatusCanonicalCode.OK
    override def description(): String = "description"
    override def durationNanos(): Long = 2000000L
    override def notes(): util.Map[String, Note[_]] = Map[String, Note[_]]("foo" -> Note.of("foo", "bar")).asJava
    override def events(): util.List[Event] = List(event).asJava
  }

  val event = new Event {
    override def name(): String = "event"
    override def attributes(): Attributes = Attributes.of(AttributeKey.stringKey("foo"), "bar")
    override def timestamp(): Long = 1234567890L
    override def exception(): Throwable = null
  }
}
