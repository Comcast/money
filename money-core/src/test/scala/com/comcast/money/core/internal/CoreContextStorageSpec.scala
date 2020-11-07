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

package com.comcast.money.core.internal

import com.comcast.money.api.Span
import io.opentelemetry.context.{ Context, ContextStorage, Scope }
import org.mockito.Mockito.{ never, reset, times, verify, when }
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class CoreContextStorageSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfter {

  val spanContext: SpanContext = mock[SpanContext]
  val mdcSupport: MDCSupport = mock[MDCSupport]
  val contextStorage: ContextStorage = mock[ContextStorage]

  before {
    reset(spanContext, mdcSupport, contextStorage)
  }

  "CoreContextStorageSpec" should {
    "current wraps ContextStorage.current" in {
      val underTest = new CoreContextStorage(spanContext, mdcSupport, contextStorage)
      val context = mock[Context]

      when(contextStorage.current).thenReturn(context)

      underTest.current shouldBe context
      verify(contextStorage).current
    }

    "sets MDC for Context on attach" in {
      val underTest = new CoreContextStorage(spanContext, mdcSupport, contextStorage)
      val toAttach = mock[Context]
      val afterAttach = mock[Context]
      val afterClose = mock[Context]
      val span = mock[Span]
      val scope = mock[Scope]

      when(contextStorage.attach(toAttach)).thenReturn(scope)
      when(contextStorage.current).thenReturn(afterAttach)
      when(spanContext.fromContext(afterAttach)).thenReturn(Some(span))

      val result = underTest.attach(toAttach)

      verify(contextStorage).attach(toAttach)
      verify(contextStorage).current
      verify(spanContext).fromContext(afterAttach)
      verify(mdcSupport).setSpanMDC(Some(span))
      verify(scope, never).close()

      when(contextStorage.current).thenReturn(afterClose)
      when(spanContext.fromContext(afterClose)).thenReturn(None)

      result.close()

      verify(scope).close()
      verify(contextStorage, times(2)).current
      verify(spanContext).fromContext(afterClose)
      verify(mdcSupport).setSpanMDC(None)
    }
  }
}
