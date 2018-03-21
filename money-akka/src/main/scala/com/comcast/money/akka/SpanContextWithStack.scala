package com.comcast.money.akka

import com.comcast.money.api.Span
import com.comcast.money.core.internal.SpanContext


class SpanContextWithStack() extends SpanContext {
  private var spans = Seq.empty[Span]

  def getAll: Seq[Span] = spans

  override def push(span: Span): Unit = spans = span +: spans

  override def pop: Option[Span] =
    spans.headOption match {
      case someSpan: Some[Span] =>
        spans = spans.tail
        someSpan

      case None => None
    }

  override def current: Option[Span] = spans.headOption

  override def clear: Unit = spans = Seq.empty[Span]
}
