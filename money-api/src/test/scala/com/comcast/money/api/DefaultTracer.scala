package com.comcast.money.api

class DefaultTracer(spanHandler: SpanHandler, spanMonitor: SpanMonitor) extends Tracer {

  override def newSpan(spanName: String): Span = newSpan(spanName, false)

  // This is hacky, don't know how yet to resolve the API to support one implementation
  // working with ThreadLocal and a different implementation not working with ThreadLocal
  // As you can see, right now we don't have
  override def newSpan(spanName: String, propagate: Boolean): Span = {
    val span = spanMonitor.inScope.map(_.childSpan(spanName, propagate)).getOrElse {
      new DefaultSpan(
        new SpanId(),
        spanName,
        spanMonitor,
        propagate = propagate
      )
    }
    span.start()
    span
  }
}
