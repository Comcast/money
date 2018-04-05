package com.comcast.money.akka.stream

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{Concat, Flow, Partition, Source, Unzip, Zip}
import com.comcast.money.akka.{MoneyExtension, SpanContextWithStack}
import com.comcast.money.core.Tracer

import scala.reflect.{ClassTag, classTag}

trait AkkaMoney {
  implicit val actorSystem: ActorSystem

  lazy val moneyExtension: MoneyExtension = MoneyExtension(actorSystem)

  def tracer(spanContext: SpanContextWithStack,
             moneyExtension: MoneyExtension): Tracer =
    moneyExtension.tracer(spanContext)
}

trait TracedStreamCombinators {
  self: AkkaMoney =>

  import akka.stream.scaladsl.GraphDSL.Implicits._

  implicit class PortOpsSpanInserter[In: ClassTag](portOps: PortOps[(In, SpanContextWithStack)])
                                                  (implicit builder: Builder[_]) {
    type TracedIn = (In, SpanContextWithStack)

    def |~>[Out: ClassTag](flow: Graph[FlowShape[In, Out], _]): PortOps[(Out, SpanContextWithStack)] =
      portOps ~> wrapFlowWithSpanFlow(flow) ~> closeSpanFlow[Out]

    def ~|[T >: In](inlet: Inlet[T]): Unit = portOps ~> closeAllSpans[In] ~> inlet

    def ~>|[Out: ClassTag](flow: Graph[FlowShape[In, Out], _]): PortOps[Out] = (portOps |~> flow) ~> closeAllSpans[Out]

    def |~\(fanIn: UniformFanInShape[TracedIn, TracedIn]): Unit = portOps ~> startSpanFlow[In](s"FanInOf${nameOfType[In]}") ~> fanIn.in(0)

    def |~/(fanIn: UniformFanInShape[TracedIn, TracedIn]): Unit = portOps ~> startSpanFlow[In](s"FanInOf${nameOfType[In]}") ~> fanIn.in(1)
  }

  implicit class SourceSpanInserter[T: ClassTag](source: Source[T, _])
                                                (implicit builder: Builder[_]) {
    type TracedIn = (T, SpanContextWithStack)

    private def createSpanContextFlow[S: ClassTag](source: Source[S, _]): Flow[S, (S, SpanContextWithStack), _] =
      Flow.fromFunction[S, (S, SpanContextWithStack)] {
        input =>
          val spanContext = new SpanContextWithStack
          tracer(spanContext, moneyExtension).startSpan("Stream")
          (input, spanContext)
      }

    def |~>[Out: ClassTag](flow: Graph[FlowShape[T, Out], _]): PortOps[(Out, SpanContextWithStack)] =
      source ~> createSpanContextFlow[T](source) |~> flow

    def |~>(fanOut: UniformFanOutShape[TracedIn, TracedIn]): Unit =
      (source ~> createSpanContextFlow[T](source)).outlet ~> startSpanFlow[T](s"FanOutOf${nameOfType[T]}") ~> fanOut.in
  }

  implicit class OutletSpanInserter[In: ClassTag](outlet: Outlet[(In, SpanContextWithStack)])
                                                 (implicit builder: Builder[_]) {
    def |~>[Out: ClassTag](flow: Graph[FlowShape[In, Out], _]): PortOps[(Out, SpanContextWithStack)] =
      outlet ~> closeSpanFlow[In] ~> wrapFlowWithSpanFlow(flow) ~> closeSpanFlow[Out]
  }

  implicit class UniformFanInConnector[T: ClassTag](fanIn: UniformFanInShape[(T, SpanContextWithStack), (T, SpanContextWithStack)])
                                                   (implicit builder: Builder[_]) {
    def ~|[S >: T](inlet: Inlet[S]): Unit = fanIn.out ~> closeAllSpans[S] ~> inlet
  }

  private def wrapFlowWithSpanFlow[In: ClassTag, Out: ClassTag](flow: Graph[FlowShape[In, Out], _])
                                                               (implicit builder: Builder[_]) = {
    val flowName = s"${nameOfType[In]}To${nameOfType[Out]}"

    val unZip = builder.add(Unzip[In, SpanContextWithStack]())
    val zip = builder.add(Zip[Out, SpanContextWithStack]())

    val spanFlowShape = builder.add(startSpanFlow[In](flowName))

    val flowShape = builder.add(flow)

    spanFlowShape.out ~> unZip.in

    unZip.out0 ~> flowShape ~> zip.in0
    unZip.out1       ~>        zip.in1

    FlowShape(spanFlowShape.in, zip.out)
  }

  private def startSpanFlow[In: ClassTag](name: String): Flow[(In, SpanContextWithStack), (In, SpanContextWithStack), _] =
    Flow[(In, SpanContextWithStack)] map {
      case (input, spanContext) =>
        tracer(spanContext, moneyExtension).startSpan(name)
        (input, spanContext)
    }

  private def closeSpanFlow[T]: Graph[FlowShape[(T, SpanContextWithStack), (T, SpanContextWithStack)], _] =
    Flow.fromFunction[(T, SpanContextWithStack), (T, SpanContextWithStack)] {
      case (input, spanContext) =>
        tracer(spanContext, moneyExtension).stopSpan()
        (input, spanContext)
    }

  private def closeAllSpans[T]: Graph[FlowShape[(T, SpanContextWithStack), T], _] =
    Flow.fromFunction[(T, SpanContextWithStack), T] {
      case (input, spanContext) =>
        spanContext.getAll.foreach(_ => tracer(spanContext, moneyExtension).stopSpan())
        input
    }

  private def nameOfType[T: ClassTag]: String = classTag[T].runtimeClass.getSimpleName
}

trait TracedBuilder {

  implicit class TracedBuilder(builder: Builder[_]) {
    def tracedAdd[T](partiton: Partition[T]): UniformFanOutShape[(T, SpanContextWithStack), (T, SpanContextWithStack)] =
      builder.add(
        graph = Partition[(T, SpanContextWithStack)](
          outputPorts = partiton.outputPorts,
          partitioner = (tWithSpan: (T, SpanContextWithStack)) => partiton.partitioner(tWithSpan._1)
        )
      )

    def tracedConcat[T, S](concat: Graph[UniformFanInShape[T, T], _]): UniformFanInShape[(T, SpanContextWithStack), (T, SpanContextWithStack)] =
      builder.add(
        graph = Concat[(T, SpanContextWithStack)](concat.shape.n)
      )
  }

}
