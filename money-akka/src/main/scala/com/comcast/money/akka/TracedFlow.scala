package com.comcast.money.akka

import akka.actor.ActorSystem
import akka.stream.{FlowShape, Inlet, Outlet, Shape}
import akka.stream.stage.{GraphStage, GraphStageLogic}
import com.comcast.money.core.Tracer

trait TracedFlow[In, Out] extends GraphStage[FlowShape[(In, SpanContextWithStack), (Out, SpanContextWithStack)]] {
  implicit val actorSystem: ActorSystem

  implicit lazy val moneyExtension: MoneyExtension = MoneyExtension(actorSystem)

  val inletName: String
  val outletName: String

  type TracedIn = (In, SpanContextWithStack)
  type TracedOut = (Out, SpanContextWithStack)

  implicit val in: Inlet[TracedIn] = Inlet(name = inletName)
  implicit val out: Outlet[TracedOut] = Outlet(name = outletName)

  override def shape: FlowShape[TracedIn, TracedOut] = FlowShape.of(in, out)
}

abstract class TracedLogic[In, Out](shape: Shape)
                                   (implicit moneyExtension: MoneyExtension) extends GraphStageLogic(shape) {
  type TracedOut = (Out, SpanContextWithStack)
  type TracedIn = (In, SpanContextWithStack)

  implicit val in: Inlet[TracedIn] = shape.inlets.head.as[TracedIn]
  implicit val out: Outlet[TracedOut] = shape.outlets.head.as[TracedOut]

  private def tracer(implicit spanContext: SpanContextWithStack,
                     moneyExtension: MoneyExtension): Tracer =
    moneyExtension.tracer(spanContext)

  def traceStageAndPush(key: String, stageLogic: In => Out): Unit = {
    implicit val (inMessage, spanContext) = grab[TracedIn](in)
    tracer.startSpan(key)
    val outMessage = stageLogic(inMessage)
    push[TracedOut](out, (outMessage, spanContext))
  }

  def traceStageAndStop(key: String, stageLogic: In => Out, isSuccessful: Boolean = true): Unit = {
    implicit val (inMessage, spanContext): (In, SpanContextWithStack) = grab[TracedIn](in)
    tracer.startSpan(key)
    val outMessage = stageLogic(inMessage)
    push[TracedOut](out, (outMessage, spanContext))
    spanContext.getAll.foreach(_ => tracer.stopSpan(isSuccessful))
  }
}
