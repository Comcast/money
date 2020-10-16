package com.comcast.money.otel

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.{ReadWriteSpan, ReadableSpan, SpanProcessor}

private [otel] object NoopSpanProcessor extends SpanProcessor {
  override def onStart(span: ReadWriteSpan): Unit = ()
  override def isStartRequired: Boolean = false
  override def onEnd(span: ReadableSpan): Unit = ()
  override def isEndRequired: Boolean = false
  override def shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
  override def forceFlush(): CompletableResultCode = CompletableResultCode.ofSuccess()
}
