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

import com.comcast.money.api.SpanId
import com.comcast.money.core.{ CoreSpan, DisabledSpanHandler }
import io.grpc.Context
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapPropagator.{ Getter, Setter }
import io.opentelemetry.trace.TracingContextUtils
import org.mockito.{ ArgumentCaptor, Mockito }
import org.mockito.Matchers.{ any, eq => argEq }
import org.mockito.Mockito.{ verify, when }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.JavaConverters._

class OtelFormatterSpec extends AnyWordSpec with MockitoSugar with Matchers {

  private val propagator = mock[TextMapPropagator]

  private val underTest = new OtelFormatter(propagator)

  "OtelFormatter" should {
    "wraps inject" in {
      val spanId = SpanId.createNew()
      val setter = mock[(String, String) => Unit]
      underTest.toHttpHeaders(spanId, setter)

      val contextCaptor = ArgumentCaptor.forClass(classOf[Context])
      val setterCaptor = ArgumentCaptor.forClass(classOf[Setter[Unit]])
      verify(propagator).inject[Unit](contextCaptor.capture(), any[Unit], setterCaptor.capture())

      val context = contextCaptor.getValue
      val span = TracingContextUtils.getSpanWithoutDefault(context)
      span should not be null
      val spanContext = span.getContext
      spanContext.getTraceIdAsHexString shouldBe spanId.traceIdAsHex
      spanContext.getSpanIdAsHexString shouldBe spanId.selfIdAsHex

      val wrappedSetter = setterCaptor.getValue
      wrappedSetter.set((), "foo", "bar")
      verify(setter).apply("foo", "bar")
    }

    "wraps extract" in {
      val spanId = SpanId.createNew()
      val span = CoreSpan(id = spanId, name = "test", handler = DisabledSpanHandler)
      val context = TracingContextUtils.withSpan(span, Context.ROOT)
      val getter = mock[String => String]

      when(propagator.extract[Unit](argEq(Context.ROOT), any[Unit], any[Getter[Unit]])).thenReturn(context)
      val result = underTest.fromHttpHeaders(getter)

      result shouldBe Some(spanId)

      val getterCaptor = ArgumentCaptor.forClass(classOf[Getter[Unit]])
      verify(propagator).extract[Unit](argEq(Context.ROOT), any[Unit], getterCaptor.capture())
      val wrappedGetter = getterCaptor.getValue

      when(getter("foo")).thenReturn("bar")
      wrappedGetter.get((), "foo") shouldBe "bar"
    }

    "wraps extract without span" in {
      val getter = mock[String => String]

      when(propagator.extract[Unit](argEq(Context.ROOT), any[Unit], any[Getter[Unit]])).thenReturn(Context.ROOT)
      val result = underTest.fromHttpHeaders(getter)

      result shouldBe None
    }

    "lists the same fields" in {
      when(propagator.fields).thenReturn(List("A", "B", "C").asJava)

      underTest.fields shouldBe Seq("A", "B", "C")
    }

    "sets the response headers" in {
      val setter = mock[(String, String) => Unit]

      when(propagator.fields).thenReturn(List("A", "B", "C").asJava)

      underTest.setResponseHeaders({
        case "A" => "1"
        case "B" => "2"
        case "C" => "3"
      }, setter)

      verify(setter).apply("A", "1")
      verify(setter).apply("B", "2")
      verify(setter).apply("C", "3")
    }
  }
}
