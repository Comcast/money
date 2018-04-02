package com.comcast.money.akka

import com.comcast.money.api.{SpanHandler, SpanInfo}

class CollectingSpanHandler() extends SpanHandler {
  var spanInfoStack = Seq.empty[SpanInfo]

  def push(span: SpanInfo): Unit = spanInfoStack = span +: spanInfoStack

  def pop: Option[SpanInfo] = spanInfoStack.headOption

  def clear(): Unit = spanInfoStack = Seq.empty[SpanInfo]

  override def handle(span: SpanInfo): Unit = push(span)
}
