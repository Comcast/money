package com.comcast.money.akka.stream

import akka.stream.{Attributes, FanOutShape, Inlet}
import akka.stream.scaladsl.{Flow, Source}

import scala.reflect.{ClassTag, classTag}


sealed trait SpanKeyCreator {
  def nameOfType[T: ClassTag]: String = classTag[T].runtimeClass.getSimpleName
}

trait FanOutSpanKeyCreator[In] extends SpanKeyCreator {
  def fanOutToKey(fanOutShape: FanOutShape[In])(implicit evIn: ClassTag[In]): String
}

object FanOutSpanKeyCreator {
  def apply[In](toKey: FanOutShape[In] => String): FanOutSpanKeyCreator[In] =
    new FanOutSpanKeyCreator[In] {
      override def fanOutToKey(fanOutShape: FanOutShape[In])(implicit evIn: ClassTag[In]): String = toKey(fanOutShape)
    }
}

trait FanInSpanKeyCreator[In] extends SpanKeyCreator {
  def fanInInletToKey(inlet: Inlet[In])(implicit evIn: ClassTag[In]): String
}

object FanInSpanKeyCreator {
  def apply[In](toKey: Inlet[In] => String): FanInSpanKeyCreator[In] =
    new FanInSpanKeyCreator[In] {
      override def fanInInletToKey(inlet: Inlet[In])(implicit evIn: ClassTag[In]): String = toKey(inlet)
    }
}

trait FlowSpanKeyCreator[In] extends SpanKeyCreator {
  def flowToKey[Out: ClassTag](flow: Flow[In, Out, _])(implicit evIn: ClassTag[In]): String
}

object FlowSpanKeyCreator {
  def apply[In](toKey: Flow[In, _, _] => String): FlowSpanKeyCreator[In] =
    new FlowSpanKeyCreator[In] {
      override def flowToKey[Out: ClassTag](flow: Flow[In, Out, _])(implicit evIn: ClassTag[In]): String = toKey(flow)
    }
}

trait SourceSpanKeyCreator[In] extends SpanKeyCreator {
  def sourceToKey(source: Source[In, _])(implicit evIn: ClassTag[In]): String
}

object SourceSpanKeyCreator {
  def apply[In](toKey: Source[In, _] => String): SourceSpanKeyCreator[In] =
    new SourceSpanKeyCreator[In] {
      override def sourceToKey(source: Source[In, _])(implicit evIn: ClassTag[In]): String = toKey(source)
    }
}

trait InletSpanKeyCreator[In] extends SpanKeyCreator {
  def inletToKey(inlet: Inlet[In])(implicit evIn: ClassTag[In]): String
}

object InletSpanKeyCreator {
  def apply[In](toKey: Inlet[In] => String): InletSpanKeyCreator[In] =
    new InletSpanKeyCreator[In] {
      override def inletToKey(inlet: Inlet[In])(implicit evIn: ClassTag[In]): String = toKey(inlet)
    }
}

private[stream] object DefaultSpanKeyCreators {

  object DefaultInletSpanKeyCreator {
    def apply[In]: InletSpanKeyCreator[In] =
      new InletSpanKeyCreator[In] {
        override def inletToKey(inlet: Inlet[In])(implicit evIn: ClassTag[In]): String = s"InletOf${nameOfType[In]}"
      }
  }

  object DefaultFanInSpanKeyCreator {
    def apply[In]: FanInSpanKeyCreator[In] =
      new FanInSpanKeyCreator[In] {
        override def fanInInletToKey(inlet: Inlet[In])(implicit evIn: ClassTag[In]): String = s"FanInOf${nameOfType[In]}"
      }
  }

  object DefaultFanOutSpanKeyCreator {
    def apply[In]: FanOutSpanKeyCreator[In] =
      new FanOutSpanKeyCreator[In] {
        override def fanOutToKey(fanOutShape: FanOutShape[In])(implicit evIn: ClassTag[In]): String = s"FanOutOf${nameOfType[In]}"
      }
  }

  object DefaultFlowSpanKeyCreator {
    def apply[In]: FlowSpanKeyCreator[In] =
      new FlowSpanKeyCreator[In] {
        override def flowToKey[Out: ClassTag](flow: Flow[In, Out, _])(implicit evIn: ClassTag[In]): String =
          Attributes.extractName(flow.traversalBuilder, s"${nameOfType[In]}To${nameOfType[Out]}")
      }
  }

  object DefaultSourceSpanKeyCreator {
    def apply[In](): SourceSpanKeyCreator[In] =
      new SourceSpanKeyCreator[In] {
        override def sourceToKey(source: Source[In, _])(implicit evIn: ClassTag[In]): String = "Stream"
      }
  }

}
