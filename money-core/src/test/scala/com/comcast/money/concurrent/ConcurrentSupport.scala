package com.comcast.money.concurrent

import java.util.concurrent.Callable

import com.comcast.money.core.SpanId
import com.comcast.money.internal.SpanLocal

trait ConcurrentSupport {

  val testCallable: Callable[Option[SpanId]] = new Callable[Option[SpanId]] with SpanAware {
    override def call(): Option[SpanId] = captureCurrentSpan()
  }

  val testRunnable: Runnable = new Runnable with SpanAware {
    override def run(): Unit = captureCurrentSpan()
  }
}

trait SpanAware {
  private var savedSpanId: Option[SpanId] = _

  def spanId: Option[SpanId] = savedSpanId

  def captureCurrentSpan(): Option[SpanId] = {
    savedSpanId = SpanLocal.current
    savedSpanId
  }
}
