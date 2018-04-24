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

import akka.stream.{ Attributes, FanOutShape, Inlet }
import akka.stream.scaladsl.{ Flow, Source }
import com.comcast.money.akka.SpanContextWithStack

import scala.reflect.{ ClassTag, classTag }

/**
 * SpanKeyCreator and the traits that inherit from it use [[ClassTag]] and the Scala reflection
 * apis to get the names of the Inlet and/or Outlet types of a Stream Shape.
 * This is then used to create a key for the [[com.comcast.money.api.Span]] that is started in that [[akka.stream.Shape]]
 *
 * A Span key is used to determine the location in the code that a Span was made. It is important that it is unique
 * where possible. If your stream has many flows with the same outward type it may be best to use [[TracedFlow]]
 *
 * [[SpanKeyCreator]] is currently a work in progress
 */
sealed trait SpanKeyCreator {
  def nameOfType[T: ClassTag]: String = classTag[T].runtimeClass.getSimpleName
}

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

trait FanOutSpanKeyCreator[In] extends SpanKeyCreator {
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

trait FanInSpanKeyCreator[In] extends SpanKeyCreator {
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

trait FlowSpanKeyCreator[In] extends SpanKeyCreator {
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

trait SourceSpanKeyCreator[Out] extends SpanKeyCreator {
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
 * @tparam In
 */

trait InletSpanKeyCreator[In] extends SpanKeyCreator {
  def inletToKey(inlet: Inlet[In])(implicit evIn: ClassTag[In]): String
}

object InletSpanKeyCreator {
  def apply[In](toKey: Inlet[In] => String): InletSpanKeyCreator[In] =
    new InletSpanKeyCreator[In] {
      override def inletToKey(inlet: Inlet[In])(implicit evIn: ClassTag[In]): String = toKey(inlet)
    }
}

/**
 * INTERNAL
 *
 * Default SpanKeyCreators to be used within the package as a way to allow for usage of the [[StreamTracingDSL]]
 * without having to define a implicit [[SpanKeyCreator]] for each [[akka.stream.Shape]] type used in the end users stream
 */

private[stream] object DefaultSpanKeyCreators {

  /**
   * Returns a string to name the Span
   *
   * Example:
   * Given In is type String
   *
   * returns "InletOfString"
   */

  object DefaultInletSpanKeyCreator {
    def apply[In]: InletSpanKeyCreator[In] =
      new InletSpanKeyCreator[In] {
        override def inletToKey(inlet: Inlet[In])(implicit evIn: ClassTag[In]): String = s"InletOf${nameOfType[In]}"
      }
  }

  /**
   * Returns a string to name the Span
   *
   * Example:
   * Given In is type String
   *
   * returns "FanInOfString"
   */

  object DefaultFanInSpanKeyCreator {
    def apply[In]: FanInSpanKeyCreator[In] =
      new FanInSpanKeyCreator[In] {
        override def fanInInletToKey(inlet: Inlet[(In, SpanContextWithStack)])(implicit evIn: ClassTag[In]): String = s"FanInOf${nameOfType[In]}"
      }
  }

  /**
   * Returns a string to name the Span
   *
   * Example:
   * Given In is type String
   *
   * returns "FanOutOfString"
   */

  object DefaultFanOutSpanKeyCreator {
    def apply[In]: FanOutSpanKeyCreator[In] =
      new FanOutSpanKeyCreator[In] {
        override def fanOutToKey(fanOutShape: FanOutShape[(In, SpanContextWithStack)])(implicit evIn: ClassTag[In]): String = s"FanOutOf${nameOfType[In]}"
      }
  }

  /**
   * Returns a string to name the [[com.comcast.money.api.Span]] for the current [[Flow]]
   *
   * NB:
   * Will default to the name provided in the [[Attributes]] of a [[Flow]]
   * It is HIGHLY recommended to use [[Flow.named]] and to allow this to name the [[com.comcast.money.api.Span]] key
   *
   * Example:
   * Given In is type String, Out is type String and there's no [[Attributes.Name]] defined for the [[Flow]]
   *
   * returns "StringToString"
   */

  object DefaultFlowSpanKeyCreator {
    def apply[In]: FlowSpanKeyCreator[In] =
      new FlowSpanKeyCreator[In] {
        override def flowToKey[Out: ClassTag](flow: Flow[In, Out, _])(implicit evIn: ClassTag[In]): String =
          Attributes.extractName(flow.traversalBuilder, s"${nameOfType[In]}To${nameOfType[Out]}")
      }
  }

  /**
   * Returns "Stream" a string to name the Span for the Stream
   *
   */

  object DefaultSourceSpanKeyCreator {
    def apply[In](): SourceSpanKeyCreator[In] =
      new SourceSpanKeyCreator[In] {
        override def sourceToKey(source: Source[In, _])(implicit evIn: ClassTag[In]): String = "Stream"
      }
  }

}
