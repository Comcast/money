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
import com.comcast.money.akka.TypeNamer.nameOfType
import com.comcast.money.akka.{ SpanContextWithStack, TypeNamer }

import scala.reflect.ClassTag

/**
 * INTERNAL
 *
 * Default SpanKeyCreators to be used within the package as a way to allow for usage of the [[StreamTracingDSL]]
 * without having to define a implicit [[TypeNamer]] for each [[akka.stream.Shape]] type used in the end users stream
 */

private[akka] object DefaultStreamSpanKeyCreators {

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
        override def inletToKey(inlet: Inlet[(In, SpanContextWithStack)])(implicit evIn: ClassTag[In]): String = s"InletOf${nameOfType[In]}"
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
