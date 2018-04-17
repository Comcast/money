package com.comcast.money.akka.stream

import akka.stream.{Attributes, FanOutShape, Inlet}
import akka.stream.scaladsl.{Flow, Source}

import scala.reflect.{ClassTag, classTag}


sealed trait SpanKeyCreator {
  def nameOfType[T: ClassTag]: String = classTag[T].runtimeClass.getSimpleName
}

trait FanOutSpanKeyCreator extends SpanKeyCreator {
  def fanOutToKey[In: ClassTag](fanOutShape: FanOutShape[In]): String
}

object FanOutSpanKeyCreator {
  def apply(toKey: FanOutShape[_] => String): FanOutSpanKeyCreator =
    new FanOutSpanKeyCreator {
      override def fanOutToKey[In: ClassTag](fanOutShape: FanOutShape[In]): String = toKey(fanOutShape)
    }
}

trait FanInSpanKeyCreator extends SpanKeyCreator {
  def fanInInletToKey[In: ClassTag](inlet: Inlet[In]): String
}

object FanInSpanKeyCreator {
  def apply(toKey: Inlet[_] => String): FanInSpanKeyCreator =
    new FanInSpanKeyCreator {
      override def fanInInletToKey[In: ClassTag](inlet: Inlet[In]): String = toKey(inlet)
    }
}

trait FlowSpanKeyCreator extends SpanKeyCreator {
  def flowToKey[In: ClassTag, Out: ClassTag](flow: Flow[In, Out, _]): String
}

object FlowSpanKeyCreator {
  def apply(toKey: Flow[_, _, _] => String): FlowSpanKeyCreator =
    new FlowSpanKeyCreator {
      override def flowToKey[In: ClassTag, Out: ClassTag](flow: Flow[In, Out, _]): String = toKey(flow)
    }
}

trait SourceSpanKeyCreator extends SpanKeyCreator {
  def sourceToKey[In: ClassTag](source: Source[In, _]): String
}

trait InletSpanKeyCreator extends SpanKeyCreator {
  def inletToKey[In: ClassTag](inlet: Inlet[In]): String
}

private[stream] object DefaultSpanKeyCreators {

  object DefaultInletSpanKeyCreator extends InletSpanKeyCreator {
    override def inletToKey[In: ClassTag](inlet: Inlet[In]): String = s"InletOf${nameOfType[In]}"
  }

  object DefaultFanInSpanKeyCreator extends FanInSpanKeyCreator {
    override def fanInInletToKey[In: ClassTag](inlet: Inlet[In]): String = s"FanInOf${nameOfType[In]}"
  }

  object DefaultFanOutSpanKeyCreator extends FanOutSpanKeyCreator {
    override def fanOutToKey[In: ClassTag](fanOutShape: FanOutShape[In]): String = s"FanOutOf${nameOfType[In]}"
  }

  object DefaultFlowSpanKeyCreator extends FlowSpanKeyCreator {
    override def flowToKey[In: ClassTag, Out: ClassTag](flow: Flow[In, Out, _]): String =
      Attributes.extractName(flow.traversalBuilder, s"${nameOfType[In]}To${nameOfType[Out]}")
  }

  object DefaultSourceSpanKeyCreator extends SourceSpanKeyCreator {
    override def sourceToKey[In: ClassTag](source: Source[In, _]): String = "Stream"
  }

}
