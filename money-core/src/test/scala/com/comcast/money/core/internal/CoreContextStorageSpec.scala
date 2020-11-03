package com.comcast.money.core.internal

import com.comcast.money.api.Span
import io.opentelemetry.context.{Context, ContextStorage, Scope}
import org.mockito.Mockito.{never, reset, times, verify, when}
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
