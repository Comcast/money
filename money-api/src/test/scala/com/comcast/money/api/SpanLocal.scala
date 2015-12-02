package com.comcast.money.api

import scala.collection._
import scala.collection.JavaConversions._

object SpanLocal {

  private val threadLocalCtx = new InheritableThreadLocal[mutable.Stack[Span]]
  threadLocalCtx.set(new mutable.Stack[Span]())

  def current: Option[Span] =
    threadLocalCtx.get().headOption

  def push(span: Span): Unit =
    threadLocalCtx.get().push(span)

  def pop(): Option[Span] =
    Option(threadLocalCtx.get().pop())

  def clear(): Unit =
    threadLocalCtx.get().clear()
}
