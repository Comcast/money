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

package com.comcast.money.akka.stream

import akka.stream.scaladsl.{ Flow, Source }
import akka.stream.{ FanOutShape, Inlet }
import com.comcast.money.akka.SpanContextWithStack

import scala.reflect.ClassTag

/**
 * FanOutSpanKeyCreator provides users the capacity to name a [[FanOutShape]] based on the type of the [[FanOutShape]]
 *
 * It is recommended for a cleaner interface to do so through the companion objects apply method like so:
 * {{{
 * implicit val stringFOSKC = FanOutSpanKeyCreator((_: FanOutShape[String]) => "SomeFanOutName")
 * }}}
 *
 * @tparam In input type of the users FanOutShape
 */

trait FanOutSpanKeyCreator[In] {
  def fanOutToKey(fanOutShape: FanOutShape[(In, SpanContextWithStack)])(implicit evIn: ClassTag[In]): String
}

object FanOutSpanKeyCreator {
  def apply[In](toKey: FanOutShape[(In, SpanContextWithStack)] => String): FanOutSpanKeyCreator[In] =
    new FanOutSpanKeyCreator[In] {
      override def fanOutToKey(fanOutShape: FanOutShape[(In, SpanContextWithStack)])(implicit evIn: ClassTag[In]): String = toKey(fanOutShape)
    }
}

/**
 * FanInSpanKeyCreator provides users the capacity to name a [[Inlet]] to a [[akka.stream.FanInShape]]
 * based on the type of the [[Inlet]]
 *
 * It is recommended for a cleaner interface to do so through the companion objects apply method like so:
 *
 * {{{
 * implicit val stringFISKC = FanInSpanKeyCreator((_: Inlet[String]) => "SomeFanInName")
 * }}}
 *
 * @tparam In input type of the users FanInShape
 */

trait FanInSpanKeyCreator[In] {
  def fanInInletToKey(inlet: Inlet[(In, SpanContextWithStack)])(implicit evIn: ClassTag[In]): String
}

object FanInSpanKeyCreator {
  def apply[In](toKey: Inlet[(In, SpanContextWithStack)] => String): FanInSpanKeyCreator[In] =
    new FanInSpanKeyCreator[In] {
      override def fanInInletToKey(inlet: Inlet[(In, SpanContextWithStack)])(implicit evIn: ClassTag[In]): String = toKey(inlet)
    }
}

/**
 * FlowSpanKeyCreator creates a key for a Flow's [[com.comcast.money.api.Span]] where the [[Flow]] accepts
 * elements of type [[In]]
 *
 * It is recommended for a cleaner interface to do so through the companion objects apply method like so:
 *
 * {{{
 *   implicit val stringFSKC = FlowSpanKeyCreator((_: Flow[String, _, _]) => "SomeFlowName")
 * }}}
 *
 * @tparam In input type of the users [[Flow]]
 */

trait FlowSpanKeyCreator[In] {
  def flowToKey[Out: ClassTag](flow: Flow[In, Out, _])(implicit evIn: ClassTag[In]): String
}

object FlowSpanKeyCreator {
  def apply[In](toKey: Flow[In, _, _] => String): FlowSpanKeyCreator[In] =
    new FlowSpanKeyCreator[In] {
      override def flowToKey[Out: ClassTag](flow: Flow[In, Out, _])(implicit evIn: ClassTag[In]): String = toKey(flow)
    }
}

/**
 * SourceSpanKeyCreator creates a key for a [[com.comcast.money.api.Span]] that represents a Full Stream.
 *
 * The Span at Source will only be closed at the end of a Stream .
 * It represent the full time taken for a individual element to pass through a stream.
 *
 * It is recommended for a cleaner interface to do so through the companion objects apply method like so:
 *
 * {{{
 *   implicit val stringSSKC = SourceSpanKeyCreator((_: Source[String, _]) => "SomeStream")
 * }}}
 *
 * @tparam Out output type of the Source
 */

trait SourceSpanKeyCreator[Out] {
  def sourceToKey(source: Source[Out, _])(implicit evIn: ClassTag[Out]): String
}

object SourceSpanKeyCreator {
  def apply[Out](toKey: Source[Out, _] => String): SourceSpanKeyCreator[Out] =
    new SourceSpanKeyCreator[Out] {
      override def sourceToKey(source: Source[Out, _])(implicit evIn: ClassTag[Out]): String = toKey(source)
    }
}

/**
 * Creates a InletSpanKeyCreator for an Inlet of type [[In]]
 *
 * {{{
 *   implicit val stringISKC = InletSpanKeyCreator((_: Inlet[String]) => "SomeInlet")
 * }}}
 *
 * @tparam In type of element accepted in the inlet
 */

trait InletSpanKeyCreator[In] {
  def inletToKey(inlet: Inlet[(In, SpanContextWithStack)])(implicit evIn: ClassTag[In]): String
}

object InletSpanKeyCreator {
  def apply[In](toKey: Inlet[(In, SpanContextWithStack)] => String): InletSpanKeyCreator[In] =
    new InletSpanKeyCreator[In] {
      override def inletToKey(inlet: Inlet[(In, SpanContextWithStack)])(implicit evIn: ClassTag[In]): String = toKey(inlet)
    }
}
