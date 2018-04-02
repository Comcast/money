package com.comcast.money.akka.stream

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{Flow, Source, Unzip, Zip}
import com.comcast.money.akka.{MoneyExtension, SpanContextWithStack}
import com.comcast.money.core.Tracer

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

  implicit class PortOpsSpanInserter[In, Out](portOps: PortOps[(In, SpanContextWithStack)])
                                             (implicit builder: Builder[_]) {

    def |~>(flow: Graph[FlowShape[In, Out], _]): PortOps[(Out, SpanContextWithStack)] = portOps ~> bounceSpanFlow(flow)

    def ~|>[T >: In](inlet: Inlet[T]): Unit = portOps ~> closingFlow ~> inlet

    private def closingFlow: Graph[FlowShape[(In, SpanContextWithStack), In], _] =
      Flow.fromFunction[(In, SpanContextWithStack), In] {
        case (input, spanContext) =>
          spanContext.getAll.foreach(_ => tracer(spanContext, moneyExtension).stopSpan())
          input
      }
  }

  implicit class SourceSpanInserter[T](source: Source[T, _])(implicit builder: Builder[_]) {

    def |~>[Out](flow: Graph[FlowShape[T, Out], _]): PortOps[(Out, SpanContextWithStack)] = source ~> spanStart(source) |~> flow

  }

  private def bounceSpanFlow[In, Out](flow: Graph[FlowShape[In, Out], _])(implicit builder: Builder[_]) = {
    val unZip = builder.add(Unzip[In, SpanContextWithStack]())
    val zip = builder.add(Zip[Out, SpanContextWithStack]())

    val spanFlowShape = builder.add(spanFlow[In](flow.toString))

    val flowShape = builder.add(flow)

    spanFlowShape.out ~> unZip.in

    unZip.out0 ~> flowShape ~> zip.in0
    unZip.out1     ~>     zip.in1


    FlowShape(spanFlowShape.in, zip.out)
  }

  private def spanFlow[T](flowName: String): Flow[(T, SpanContextWithStack), (T, SpanContextWithStack), _] =
    Flow[(T, SpanContextWithStack)] map {
      case (input, spanContext) =>
        tracer(spanContext, moneyExtension).startSpan(flowName)
        (input, spanContext)
    }

  private def spanStart[T](source: Source[T, _]): Flow[T, (T, SpanContextWithStack), _] =
    Flow.fromFunction[T, (T, SpanContextWithStack)] {
      input =>
        val spanContext = new SpanContextWithStack
        tracer(spanContext, moneyExtension).startSpan(s"${source.toString}")
        (input, spanContext)
    }
}
