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

import com.comcast.money.api.{ InstrumentationLibrary, Note, SpanHandler, SpanId }
import com.comcast.money.core.formatters.MoneyTraceFormatter
import com.comcast.money.core.handlers.TestData
import com.comcast.money.core.samplers.{ AlwaysOffSampler, AlwaysOnSampler, RecordResult, Sampler, SamplerResult }
import io.opentelemetry.trace.TraceFlags
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class CoreSpanFactorySpec extends AnyWordSpec with Matchers with MockitoSugar with TestData {

  val handler = mock[SpanHandler]
  val formatter = new MoneyTraceFormatter()
  val sampler = AlwaysOnSampler
  val instrumentationLibrary = new InstrumentationLibrary("test", "0.0.1")
  val underTest = CoreSpanFactory(clock, handler, formatter, sampler, instrumentationLibrary)

  "CoreSpanFactory" should {
    "create a new span" in {
      val result = underTest.newSpan("foo").asInstanceOf[CoreSpan]

      result.info.name shouldBe "foo"
      result.handler shouldBe handler
    }

    "create a new span given an existing span id" in {
      val existingId = SpanId.createNew()
      val result = underTest.newSpan(existingId, "foo").asInstanceOf[CoreSpan]

      result.id shouldBe existingId
    }

    "create a child span whos id descends from an existing span" in {
      val result = underTest.childSpan("child", testSpan)

      val parent = testSpan.info
      val child = result.info

      child.id.traceId shouldBe parent.id.traceId
      child.id.parentId shouldBe parent.id.selfId
      child.id.selfId == parent.id.selfId shouldBe false
    }

    "propagate sticky notes to a child span" in {

      val parentSpan = underTest.newSpan("parent")
      val stickyNote = Note.of("foo", "bar", true)
      val nonStickyNote = Note.of("other", "one", false)
      parentSpan.record(stickyNote)
      parentSpan.record(nonStickyNote)

      val childSpan = underTest.childSpan("child", parentSpan, true)
      val childInfo = childSpan.info

      childInfo.notes should contain value stickyNote
      childInfo.notes shouldNot contain value nonStickyNote
    }

    "create a child span from a well-formed x-moneytrace header" in {
      val parentSpan = underTest.newSpan("parent")

      formatter.toHttpHeaders(parentSpan.info.id, (headerName, headerValue) => headerName match {
        case MoneyTraceFormatter.MoneyTraceHeader => {
          val childSpan = underTest.newSpanFromHeader("child", _ => headerValue)

          childSpan.info.id.traceId shouldBe parentSpan.info.id.traceId
          childSpan.info.id.parentId shouldBe parentSpan.info.id.selfId
          childSpan.info.id.selfId == parentSpan.info.id.selfId shouldBe false
        }
        case _ =>
      })
    }

    "create a root span from a malformed x-moneytrace header" in {
      val parentSpan = underTest.newSpan("parent")
      val traceContextHeader = "mangled header value"
      val childSpan = underTest.newSpanFromHeader("child", headerName => traceContextHeader)

      childSpan.info.id.traceId == parentSpan.info.id.traceId shouldBe false
      childSpan.info.id.parentId == parentSpan.info.id.selfId shouldBe false
      childSpan.info.id.selfId == parentSpan.info.id.selfId shouldBe false
      childSpan.info.id.selfId shouldBe childSpan.info.id.parentId
    }

    "creates an unrecorded span when the sampler drops the span" in {
      val underTest = this.underTest.copy(sampler = AlwaysOffSampler)
      val span = underTest.newSpan("test")

      span shouldBe a[UnrecordedSpan]
    }

    "sets the trace flags to not sample when the sample only records the span" in {
      val sampler = new Sampler {
        override def shouldSample(spanId: SpanId, parentSpanId: Option[SpanId], spanName: String): SamplerResult =
          RecordResult(sample = false)
      }
      val underTest = this.underTest.copy(sampler = sampler)
      val span = underTest.newSpan("test")

      span.info.id.isSampled shouldBe false
      span.info.id.traceFlags shouldBe TraceFlags.getDefault
    }

    "records sampler notes on the span" in {
      val note = Note.of("foo", "bar", true, 0L)
      val underTest = this.underTest.copy(sampler = new SamplerWithNote(note))
      val span = underTest.newSpan("test")

      val notes = span.info.notes
      notes should have size 1
      notes should contain key "foo"
      notes should contain value note
    }
  }

  class SamplerWithNote(note: Note[_]) extends Sampler {
    override def shouldSample(spanId: SpanId, parentSpanId: Option[SpanId], spanName: String): SamplerResult =
      RecordResult(notes = Seq(note))
  }
}
