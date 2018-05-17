/*
 * Copyright 2012-2015 Comcast Cable Communications Management, LLC
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

package com.comcast.money.akka

import com.comcast.money.core.Tracer

/**
 * [[TraceContext]] follows subject being traced to allow for more efficient access
 *
 * @param tracer      The [[Tracer]] to stop and start spans stored on the [[SpanContextWithStack]]
 * @param spanContext [[SpanContextWithStack]] that maintains a stack of spans for subject of trace
 */

case class TraceContext(tracer: Tracer, spanContext: SpanContextWithStack)

/**
 * Companion object for creating a [[TraceContext]]
 */

object TraceContext {
  def apply(spanContext: SpanContextWithStack)(implicit moneyExtension: MoneyExtension): TraceContext =
    TraceContext(moneyExtension.tracer(spanContext), spanContext)
}

/**
 * Companion object for creating a [[TraceContext]] where multiple elements will use the same parent
 */

object FreshTraceContext {

  /**
   * Constructs a [[TraceContext]] with an implicit [[MoneyExtension]]
   *
   * @param spanContextWithStack [[SpanContextWithStack]] maintains a stack of Spans
   * @param moneyExtension       connects to [[com.comcast.money.core.Money]] through the [[akka.actor.ActorSystem]]
   * @return TraceContext
   */

  def apply(spanContextWithStack: SpanContextWithStack)(implicit moneyExtension: MoneyExtension): TraceContext =
    createFreshTraceContext(moneyExtension, spanContextWithStack)

  /**
   * Returns [[TraceContext]] with a copy of the passed [[SpanContextWithStack]]
   *
   * Copies the [[SpanContextWithStack]] so that for example when being used in an Akka Stream
   * a fresh copy is created for each element that passes through the stream.
   *
   * @param moneyExtension
   * @param spanContextWithStack
   * @return
   */

  private def createFreshTraceContext(moneyExtension: MoneyExtension, spanContextWithStack: SpanContextWithStack): TraceContext = {
    val copiedSpanContext = spanContextWithStack.copy
    TraceContext(moneyExtension.tracer(copiedSpanContext), copiedSpanContext)
  }
}
