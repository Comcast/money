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

package com.comcast.money.core.context

import com.comcast.money.api.{ Span, SpanId, SpanInfo }
import com.comcast.money.core.internal.SpanContext
import com.typesafe.config.ConfigFactory
import io.opentelemetry.context.{ Context, ContextStorage, Scope }
import org.mockito.Mockito.{ times, verify, when }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.spi.MDCAdapter

class StructuredMdcContextStorageFilterSpec extends AnyWordSpec with Matchers with MockitoSugar {

  "StructuredMdcContextStorageFilter" should {
    "puts root span info in MDC" in {
      val spanContext = mock[SpanContext]
      val mdc = mock[MDCAdapter]
      val config = ConfigFactory.empty
      val context = mock[Context]
      val previousContext = mock[Context]
      val contextStorage = mock[ContextStorage]
      val scope = mock[Scope]
      val span = mock[Span]
      val spanInfo = mock[SpanInfo]
      val spanId = SpanId.createNew()

      val underTest = StructuredMdcContextStorageFilter(config, spanContext, mdc)

      when(contextStorage.attach(context)).thenReturn(scope)
      when(spanContext.fromContext(context)).thenReturn(Some(span))
      when(span.info).thenReturn(spanInfo)
      when(spanInfo.id).thenReturn(spanId)
      when(spanInfo.name).thenReturn("spanName")

      val result = underTest.attach(context, contextStorage)

      verify(contextStorage).attach(context)
      verify(mdc).put("trace-id", spanId.traceId)
      verify(mdc).put("span-id", spanId.selfId.toString)
      verify(mdc).put("parent-id", spanId.parentId.toString)
      verify(mdc).put("span-name", "spanName")

      when(contextStorage.current()).thenReturn(previousContext)
      when(spanContext.fromContext(previousContext)).thenReturn(None)

      result.close()

      verify(scope).close()
      verify(mdc).remove("trace-id")
      verify(mdc).remove("span-id")
      verify(mdc).remove("parent-id")
      verify(mdc).remove("span-name")
    }

    "puts child span info in MDC" in {
      val spanContext = mock[SpanContext]
      val mdc = mock[MDCAdapter]
      val config = ConfigFactory.empty
      val context = mock[Context]
      val previousContext = mock[Context]
      val contextStorage = mock[ContextStorage]
      val scope = mock[Scope]
      val parentSpan = mock[Span]
      val parentSpanInfo = mock[SpanInfo]
      val parentSpanId = SpanId.createNew()
      val childSpan = mock[Span]
      val childSpanInfo = mock[SpanInfo]
      val childSpanId = parentSpanId.createChild()

      val underTest = StructuredMdcContextStorageFilter(config, spanContext, mdc)

      when(contextStorage.attach(context)).thenReturn(scope)
      when(spanContext.fromContext(context)).thenReturn(Some(childSpan))
      when(childSpan.info).thenReturn(childSpanInfo)
      when(childSpanInfo.id).thenReturn(childSpanId)
      when(childSpanInfo.name).thenReturn("childName")

      val result = underTest.attach(context, contextStorage)

      verify(contextStorage).attach(context)
      verify(mdc).put("trace-id", childSpanId.traceId)
      verify(mdc).put("span-id", childSpanId.selfId.toString)
      verify(mdc).put("parent-id", childSpanId.parentId.toString)
      verify(mdc).put("span-name", "childName")

      when(contextStorage.current()).thenReturn(previousContext)
      when(spanContext.fromContext(previousContext)).thenReturn(Some(parentSpan))
      when(parentSpan.info).thenReturn(parentSpanInfo)
      when(parentSpanInfo.id).thenReturn(parentSpanId)
      when(parentSpanInfo.name).thenReturn("parentName")

      result.close()

      verify(scope).close()
      verify(mdc, times(2)).put("trace-id", parentSpanId.traceId)
      verify(mdc).put("span-id", parentSpanId.selfId.toString)
      verify(mdc, times(2)).put("parent-id", parentSpanId.parentId.toString)
      verify(mdc).put("span-name", "parentName")
    }

    "puts root span info in MDC with hex formatting" in {
      val spanContext = mock[SpanContext]
      val mdc = mock[MDCAdapter]
      val config = ConfigFactory.parseString(
        """
          | format-ids-as-hex = true
          |""".stripMargin)
      val context = mock[Context]
      val previousContext = mock[Context]
      val contextStorage = mock[ContextStorage]
      val scope = mock[Scope]
      val span = mock[Span]
      val spanInfo = mock[SpanInfo]
      val spanId = SpanId.createNew()

      val underTest = StructuredMdcContextStorageFilter(config, spanContext, mdc)

      when(contextStorage.attach(context)).thenReturn(scope)
      when(spanContext.fromContext(context)).thenReturn(Some(span))
      when(span.info).thenReturn(spanInfo)
      when(spanInfo.id).thenReturn(spanId)
      when(spanInfo.name).thenReturn("spanName")

      val result = underTest.attach(context, contextStorage)

      verify(contextStorage).attach(context)
      verify(mdc).put("trace-id", spanId.traceIdAsHex)
      verify(mdc).put("span-id", spanId.selfIdAsHex)
      verify(mdc).put("parent-id", spanId.parentIdAsHex)
      verify(mdc).put("span-name", "spanName")

      when(contextStorage.current()).thenReturn(previousContext)
      when(spanContext.fromContext(previousContext)).thenReturn(None)

      result.close()

      verify(scope).close()
      verify(mdc).remove("trace-id")
      verify(mdc).remove("span-id")
      verify(mdc).remove("parent-id")
      verify(mdc).remove("span-name")
    }

    "puts root span info in MDC with custom key names" in {
      val spanContext = mock[SpanContext]
      val mdc = mock[MDCAdapter]
      val config = ConfigFactory.parseString(
        """
          | trace-id = "trace.id"
          | span-id = "self.id"
          | parent-id = "parent.id"
          | span-name = "name"
          |""".stripMargin)
      val context = mock[Context]
      val previousContext = mock[Context]
      val contextStorage = mock[ContextStorage]
      val scope = mock[Scope]
      val span = mock[Span]
      val spanInfo = mock[SpanInfo]
      val spanId = SpanId.createNew()

      val underTest = StructuredMdcContextStorageFilter(config, spanContext, mdc)

      when(contextStorage.attach(context)).thenReturn(scope)
      when(spanContext.fromContext(context)).thenReturn(Some(span))
      when(span.info).thenReturn(spanInfo)
      when(spanInfo.id).thenReturn(spanId)
      when(spanInfo.name).thenReturn("spanName")

      val result = underTest.attach(context, contextStorage)

      verify(contextStorage).attach(context)
      verify(mdc).put("trace.id", spanId.traceId)
      verify(mdc).put("self.id", spanId.selfId.toString)
      verify(mdc).put("parent.id", spanId.parentId.toString)
      verify(mdc).put("name", "spanName")

      when(contextStorage.current()).thenReturn(previousContext)
      when(spanContext.fromContext(previousContext)).thenReturn(None)

      result.close()

      verify(scope).close()
      verify(mdc).remove("trace.id")
      verify(mdc).remove("self.id")
      verify(mdc).remove("parent.id")
      verify(mdc).remove("name")
    }
  }
}
