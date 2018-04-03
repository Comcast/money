package com.comcast.money.akka.stream

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{Flow, Source, Unzip, Zip}
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

trait TracingStreamCombinators {
  self: AkkaMoney =>

  import akka.stream.scaladsl.GraphDSL.Implicits._

  implicit class PortOpsSpanInserter[In](portOps: PortOps[(In, SpanContextWithStack)])
                                        (implicit builder: Builder[_], evidence: ClassTag[In]) {

    def |~>[Out: ClassTag](flow: Graph[FlowShape[In, Out], _]): PortOps[(Out, SpanContextWithStack)] = portOps ~> wrapWithSpanFlow(flow) ~> closeSpanFlow[Out]

    def ~|[T >: In](inlet: Inlet[T]): Unit = portOps ~> closeAllFlow[In] ~> inlet

    def ~>|[Out: ClassTag](flow: Graph[FlowShape[In, Out], _]): PortOps[Out] = (portOps |~> flow) ~> closeAllFlow[Out]

    private def closeAllFlow[T]: Graph[FlowShape[(T, SpanContextWithStack), T], _] =
      Flow.fromFunction[(T, SpanContextWithStack), T] {
        case (input, spanContext) =>
          spanContext.getAll.foreach(_ => tracer(spanContext, moneyExtension).stopSpan())
          input
      }

    private def closeSpanFlow[T]: Graph[FlowShape[(T, SpanContextWithStack), (T, SpanContextWithStack)], _] =
      Flow.fromFunction[(T, SpanContextWithStack), (T, SpanContextWithStack)] {
        case (input, spanContext) =>
          tracer(spanContext, moneyExtension).stopSpan()
          (input, spanContext)
      }
  }

  implicit class SourceSpanInserter[T](source: Source[T, _])
                                      (implicit builder: Builder[_],
                                       evidence: ClassTag[T]) {

    private def spanStart[S: ClassTag](source: Source[S, _]): Flow[S, (S, SpanContextWithStack), _] =
      Flow.fromFunction[S, (S, SpanContextWithStack)] {
        input =>
          val spanContext = new SpanContextWithStack
          tracer(spanContext, moneyExtension).startSpan(s"SourceOf${nameOfType[S]}")
          (input, spanContext)
      }

    def |~>[Out: ClassTag](flow: Graph[FlowShape[T, Out], _]): PortOps[(Out, SpanContextWithStack)] = source ~> spanStart[T](source) |~> flow

  }

  private def wrapWithSpanFlow[In: ClassTag, Out: ClassTag](flow: Graph[FlowShape[In, Out], _])
                                                           (implicit builder: Builder[_]) = {
    val flowName = s"${nameOfType[In]}To${nameOfType[Out]}"

    def spanFlow[T](flowName: String): Flow[(T, SpanContextWithStack), (T, SpanContextWithStack), _] =
      Flow[(T, SpanContextWithStack)] map {
        case (input, spanContext) =>
          tracer(spanContext, moneyExtension).startSpan(flowName)
          (input, spanContext)
      }

    val unZip = builder.add(Unzip[In, SpanContextWithStack]())
    val zip = builder.add(Zip[Out, SpanContextWithStack]())

    val spanFlowShape = builder.add(spanFlow[In](flowName))

    val flowShape = builder.add(flow)

    spanFlowShape.out ~> unZip.in

    unZip.out0 ~> flowShape ~> zip.in0
    unZip.out1       ~>        zip.in1

    FlowShape(spanFlowShape.in, zip.out)
  }

  private def nameOfType[T: ClassTag]: String = classTag[T].runtimeClass.getSimpleName
}
