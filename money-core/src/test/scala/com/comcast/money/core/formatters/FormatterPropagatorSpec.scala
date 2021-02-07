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

package com.comcast.money.core.formatters

import com.comcast.money.api.{ IdGenerator, Span, SpanId }
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.api.trace.{ TraceFlags, TraceState, Span => OtelSpan }
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{ verify, when }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class FormatterPropagatorSpec extends AnyWordSpec with MockitoSugar with Matchers {

  "FormatterPropagator" should {
    "delegates Formatter.fields to TextMapPropagator.fields" in {
      val formatter = mock[Formatter]
      val underTest = FormatterPropagator(formatter)

      when(formatter.fields).thenReturn(Seq("A", "B", "C"))

      val fields = underTest.fields()

      fields should contain theSameElementsInOrderAs Seq("A", "B", "C")
    }

    "delegates Formatter.toHttpHeaders to TextMapPropagator.inject" in {
      val formatter = mock[Formatter]
      val setter = mock[TextMapPropagator.Setter[Unit]]
      val underTest = FormatterPropagator(formatter)

      val spanId = SpanId.createNew()
      val span = OtelSpan.wrap(spanId.toSpanContext)
      val context = span.storeInContext(Context.root)

      underTest.inject[Unit](context, (), setter)

      val captor = ArgumentCaptor.forClass(classOf[SpanId])
      verify(formatter).toHttpHeaders(captor.capture(), any())

      val capturedSpanId = captor.getValue

      capturedSpanId shouldBe spanId
    }

    "delegates Formatter.fromHttpHeaders to TextMapPropagator.extract" in {
      val formatter = mock[Formatter]
      val getter = mock[TextMapPropagator.Getter[Unit]]
      val underTest = FormatterPropagator(formatter)

      val traceId = IdGenerator.generateRandomTraceId()
      val selfId = IdGenerator.generateRandomId();
      val spanId = SpanId.createRemote(traceId, selfId, selfId, TraceFlags.getSampled, TraceState.getDefault)

      when(formatter.fromHttpHeaders(any(), any(), any())).thenReturn(Some(spanId))

      val context = underTest.extract[Unit](Context.root, (), getter)
      val span = OtelSpan.fromContextOrNull(context)

      span should not be null
      val spanContext = span.getSpanContext
      spanContext.getTraceIdHex shouldBe spanId.traceIdAsHex
      spanContext.getSpanIdHex shouldBe spanId.selfIdAsHex
      spanContext.getTraceFlags shouldBe spanId.traceFlags
      spanContext.isRemote shouldBe true
    }

    "delegates Formatter.fromHttpHeaders to TextMapPropagator.extract without span" in {
      val formatter = mock[Formatter]
      val getter = mock[TextMapPropagator.Getter[Unit]]
      val underTest = FormatterPropagator(formatter)

      val traceId = IdGenerator.generateRandomTraceId()
      val selfId = IdGenerator.generateRandomId();
      val spanId = SpanId.createRemote(traceId, selfId, selfId, TraceFlags.getSampled, TraceState.getDefault)

      when(formatter.fromHttpHeaders(any(), any(), any())).thenReturn(None)

      val context = underTest.extract[Unit](Context.root(), (), getter)
      val span = OtelSpan.fromContextOrNull(context)

      span shouldBe null
    }
  }
}
