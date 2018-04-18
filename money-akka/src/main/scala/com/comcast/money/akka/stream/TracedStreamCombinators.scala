package com.comcast.money.akka.stream

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{Concat, Flow, GraphDSL, Partition, Source, Unzip, Zip}
import com.comcast.money.akka.{MoneyExtension, SpanContextWithStack}
import com.comcast.money.core.Tracer

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait AkkaMoney {
  implicit val actorSystem: ActorSystem

  lazy val moneyExtension: MoneyExtension = MoneyExtension(actorSystem)

  def tracer(spanContext: SpanContextWithStack,
             moneyExtension: MoneyExtension): Tracer =
    moneyExtension.tracer(spanContext)
}

trait TracedStreamCombinators {
  self: AkkaMoney =>

  import DefaultSpanKeyCreators._
  import akka.stream.scaladsl.GraphDSL.Implicits._

  implicit class PortOpsSpanInserter[In: ClassTag](portOps: PortOps[(In, SpanContextWithStack)])
                                                  (implicit builder: Builder[_],
                                                   fskc: FlowSpanKeyCreator[In] = DefaultFlowSpanKeyCreator[In]) {
    type TracedIn = (In, SpanContextWithStack)

    def ~|>[Out: ClassTag](flow: Flow[In, Out, _]): PortOps[(Out, SpanContextWithStack)] =
      portOps ~> wrapFlowWithSpanFlow(flow) ~> closeSpanFlow[Out]

    def ~|>[T >: TracedIn : ClassTag](inlet: Inlet[T])
                                     (implicit iskc: InletSpanKeyCreator[T] = DefaultInletSpanKeyCreator[T]): Unit =
      portOps ~> startSpanFlow[In](iskc.inletToKey(inlet)) ~> inlet

    def ~<>[T >: TracedIn : ClassTag](inlet: Inlet[T])
                                     (implicit fisck: FanInSpanKeyCreator[T] = DefaultFanInSpanKeyCreator[T]): Unit =
      portOps ~> startSpanFlow[In](fisck.fanInInletToKey(inlet)) ~> inlet

    def ~|[T >: In](inlet: Inlet[T]): Unit = portOps ~> closeAllSpans[In] ~> inlet

    def ~|~[Out: ClassTag](flow: Flow[In, Out, _]): PortOps[Out] =
      portOps ~|> flow ~> closeAllSpans[Out]
  }

  implicit class SourceSpanInserter[In: ClassTag](source: Source[In, _])
                                                 (implicit builder: Builder[_],
                                                  sskc: SourceSpanKeyCreator[In] = DefaultSourceSpanKeyCreator[In],
                                                  fskc: FlowSpanKeyCreator[In] = DefaultFlowSpanKeyCreator[In]) {
    type TracedIn = (In, SpanContextWithStack)

    private def createSpanContextFlow(source: Source[In, _]): Flow[In, (In, SpanContextWithStack), _] =
      Flow.fromFunction[In, (In, SpanContextWithStack)] {
        input =>
          val spanContext = new SpanContextWithStack
          tracer(spanContext, moneyExtension).startSpan(sskc.sourceToKey(source))
          (input, spanContext)
      }

    def ~|>[Out: ClassTag](flow: Flow[In, Out, _]): PortOps[(Out, SpanContextWithStack)] =
      source ~> createSpanContextFlow(source) ~|> flow

    def ~|>[TracedOut <: (_, SpanContextWithStack)](flow: Graph[FlowShape[TracedIn, TracedOut], _]): PortOps[TracedOut] =
      source ~> createSpanContextFlow(source) ~> flow

    def ~|>(fanOut: UniformFanOutShape[TracedIn, TracedIn])
           (implicit foskc: FanOutSpanKeyCreator[TracedIn] = DefaultFanOutSpanKeyCreator[TracedIn]): Unit =
      (source ~> createSpanContextFlow(source)).outlet ~> startSpanFlow[In](foskc.fanOutToKey(fanOut)) ~> fanOut.in
  }

  implicit class OutletSpanInserter[In: ClassTag](outlet: Outlet[(In, SpanContextWithStack)])
                                                 (implicit builder: Builder[_]) {
    def ~|>[Out: ClassTag](flow: Flow[In, Out, _])
                          (implicit fskc: FlowSpanKeyCreator[In] = DefaultFlowSpanKeyCreator[In]): PortOps[(Out, SpanContextWithStack)] =
      outlet ~> closeSpanFlow[In] ~> wrapFlowWithSpanFlow(flow) ~> closeSpanFlow[Out]
  }

  implicit class UniformFanInConnector[T: ClassTag](fanIn: UniformFanInShape[(T, SpanContextWithStack), (T, SpanContextWithStack)])
                                                   (implicit builder: Builder[_]) {
    def ~|[S >: T](inlet: Inlet[S]): Unit = fanIn.out ~> closeAllSpans[S] ~> inlet
  }

  implicit class TracedFlowOps[In: ClassTag, Out: ClassTag](flow: Flow[In, Out, _])
                                                           (implicit executionContext: ExecutionContext,
                                                            fskc: FlowSpanKeyCreator[In] = DefaultFlowSpanKeyCreator[In]) {
    type TracedIn = (In, SpanContextWithStack)
    type TracedOut = (Out, SpanContextWithStack)

    def tracedMapAsyncUnordered(parallelism: Int)(f: In => Future[Out]) =
      Flow[TracedIn].mapAsyncUnordered[TracedOut](parallelism) {
        (tuple: TracedIn) =>
          val (in, spanContext) = tuple
          tracer(spanContext, moneyExtension).startSpan(fskc.flowToKey(flow))
          f(in) flatMap {
            out =>
              Future {
                tracer(spanContext, moneyExtension).stopSpan()
                (out, spanContext)
              }
          }
      }
  }

  private def wrapFlowWithSpanFlow[In: ClassTag, Out: ClassTag](flow: Flow[In, Out, _])
                                                               (implicit builder: Builder[_],
                                                                fskc: FlowSpanKeyCreator[In]) =
    Flow fromGraph {
      GraphDSL.create() {
        implicit builder: Builder[_] =>
          import akka.stream.scaladsl.GraphDSL.Implicits._

          val unZip = builder.add(Unzip[In, SpanContextWithStack]().async)
          val zip = builder.add(Zip[Out, SpanContextWithStack]())

          val spanFlowShape = builder.add(startSpanFlow[In](fskc.flowToKey(flow)))

          val flowShape = builder.add(flow)

          spanFlowShape.out ~> unZip.in

          unZip.out0 ~> flowShape ~> zip.in0
          unZip.out1       ~>        zip.in1
          FlowShape(spanFlowShape.in, zip.out)
      }
    } withAttributes flow.traversalBuilder.attributes

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
}

trait TracedBuilder {

  implicit class TracedBuilder(builder: Builder[_]) {
    def tracedAdd[T](partition: Partition[T]) =
      builder.add(
        graph = Partition[(T, SpanContextWithStack)](
          outputPorts = partition.outputPorts,
          partitioner = (tWithSpan: (T, SpanContextWithStack)) => partition.partitioner(tWithSpan._1)
        )
      )

    def tracedConcat[T](concat: Graph[UniformFanInShape[T, T], _]) =
      builder.add(
        graph = Concat[(T, SpanContextWithStack)](concat.shape.n)
      )
  }

}
