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

package com.comcast.money.core.akka

import scala.collection.mutable.{ Buffer }
import scala.collection.immutable.{ Stack }
import akka.actor.{ Actor, ActorSystem }
import com.comcast.money.api.Span
import com.comcast.money.core.Tracer
import com.comcast.money.core.internal.SpanContext

import scala.collection.IterableLike

/**
 * This class provides a stack-like structure to collect Spans.
 * This allows to refer to sub-spans and keeping all the storage in this class
 */
trait SpanCarrier extends SpanContext with IterableLike[SpanCarrier, SpanCarrier] {
  protected[akka] var spanId: Stack[Span] = Stack()
  protected[akka] var parent: Option[SpanCarrier] = None

  override def current: Option[Span] = spanId.headOption orElse (parent.flatMap(_.current))

  override def pop(): Option[Span] = {
    val filledSpanId = iterator.find(spanCarrier => !spanCarrier.spanId.isEmpty)
    filledSpanId.flatMap(spanCarrier => {
      val retValue = spanCarrier.spanId.headOption
      filledSpanId.get.spanId = spanCarrier.spanId.drop(1)
      retValue
    })
  }

  override def push(span: Span): Unit = {
    spanId = spanId.push(span)
  }

  override def clear() = {
    spanId = Stack()
    parent = None
  }

  def iterator: Iterator[SpanCarrier] = new Iterator[SpanCarrier]() {
    var it: Option[SpanCarrier] = Some(SpanCarrier.this)

    def next(): SpanCarrier = {
      val retValue = it
      it = it.flatMap(_.parent)
      retValue.get
    }

    def hasNext(): Boolean = it.isDefined
  }

  def seq: scala.collection.TraversableOnce[SpanCarrier] = {
    val buf = Buffer[SpanCarrier]()
    val it = iterator
    while (it.hasNext) {
      buf.append(it.next())
    }
    buf
  }

  protected[this] def newBuilder: scala.collection.mutable.Builder[SpanCarrier, SpanCarrier] = ???

  override def addString(b: StringBuilder, start: String, sep: String, end: String): StringBuilder = {
    b append start
    b append spanId
    b append sep
    b append parent
    b append end
  }
}

object SpanCarrier {
  /** Just a global SpanCarrier to be used in test cases or simmilar */
  def root: SpanCarrier = Implicits.root

  /* Pattern inspired by scala.concurrent.ExecutionContext */
  object Implicits {
    implicit lazy val root: SpanCarrier = new RootSpanCarrier
  }

  def tracing[T](spanName: String, f: (Tracer) => T)(implicit spanCarrier: SpanCarrier, system: ActorSystem): T = {
    val tracer = MoneyExtension(system).tracer(spanCarrier)
    tracer.startSpan(spanName)
    try f(tracer) finally tracer.stopSpan(true)
  }
}

/**
 * Use this abstract class to extend your existing message case classes with.
 * This allows the usage of the {@code tracer()} in {@code MoneyActor}.
 *
 * It is a stack-like structure with a parent stack.
 */
abstract class BaseSpanCarrier(implicit parentSpanCarrier: SpanCarrier) extends SpanCarrier {
  parent = Some(parentSpanCarrier)
}

/** Wrapper around {@code SpanCarrier} for better naming */
class RootSpanCarrier extends SpanCarrier {
}

trait MoneyActor {
  self: Actor =>

  private lazy val moneyExtension = MoneyExtension(context.system)

  // Exposing Money functionality to the actor
  def tracer(implicit spanCarrier: SpanCarrier) = moneyExtension.tracer(spanCarrier)

}
