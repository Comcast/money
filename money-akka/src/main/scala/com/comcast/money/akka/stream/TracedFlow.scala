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

import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import com.comcast.money.akka.{ MoneyExtension, TraceContext }

/**
 * TracedFlow is used to create a traced version of a user defined [[akka.stream.scaladsl.Flow]]
 * it lessens the amount of boilerplate required to construct a new [[akka.stream.scaladsl.Flow]]
 *
 * @tparam In  input type of the users Flow
 * @tparam Out output type of the users Flow
 */

trait TracedFlow[In, Out] extends GraphStage[FlowShape[(In, TraceContext), (Out, TraceContext)]] {
  val _inletName: String
  val _outletName: String

  type TracedIn = (In, TraceContext)
  type TracedOut = (Out, TraceContext)

  val in: Inlet[TracedIn] = Inlet(name = _inletName)
  val out: Outlet[TracedOut] = Outlet(name = _outletName)

  override def shape: FlowShape[TracedIn, TracedOut] = FlowShape.of(in, out)

  implicit val flowShape: FlowShape[TracedIn, TracedOut] = shape

  val _pushLogic: PushLogic[In, Out]

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = TraceRetainingFlowLogic(_pushLogic)
}

object TracedFlow {
  def apply[In, Out](outletName: String, inletName: String, pushLogic: PushLogic[In, Out]): TracedFlow[In, Out] =
    new TracedFlow[In, Out] {
      override val _inletName: String = inletName
      override val _outletName: String = outletName
      override val _pushLogic: PushLogic[In, Out] = pushLogic
    }
}

sealed trait TracedFlowLogic[In, Out] {
  type TracedIn = (In, TraceContext)
  type TracedOut = (Out, TraceContext)
}

/**
 * TracedRetainingFlowLogic is a wrapper interface to [[GraphStageLogic]] for a [[TracedFlow]]
 * it provides functionality for tracing and executing the Flows logic
 *
 * @param flowShape the traced shape of the Flow being traced
 * @tparam In  input type of the users Flow
 * @tparam Out output type of the users Flow
 */

abstract class TraceRetainingFlowLogic[In, Out](implicit flowShape: FlowShape[(In, TraceContext), (Out, TraceContext)]) extends GraphStageLogic(flowShape) with TracedFlowLogic[In, Out] {
  val in: Inlet[TracedIn] = flowShape.in
  val out: Outlet[TracedOut] = flowShape.out

  /**
   * returns Unit
   *
   * Pushes an element down the stream tracing the logic passed to it.
   * All logic to be traced must be passed as stageLogic
   *
   * @param pushLogic contains the [[com.comcast.money.api.Span]] key and logic to be applied to each element
   */

  def tracedPush(pushLogic: PushLogic[In, Out]): Unit = {

    implicit val (inMessage, traceContext) = grab[TracedIn](in)

    traceContext.tracer.startSpan(pushLogic.key)

    val (outMessage, isSuccessful) = pushLogic.inToOutWithIsSuccessful(inMessage)

    push[TracedOut](out, (outMessage, traceContext))

    if (pushLogic.shouldStop) traceContext.tracer.stopSpan(isSuccessful)
  }
}

object TraceRetainingFlowLogic {
  def apply[In, Out](pushLogic: PushLogic[In, Out])(implicit flowShape: FlowShape[(In, TraceContext), (Out, TraceContext)]): TraceRetainingFlowLogic[In, Out] =
    new TraceRetainingFlowLogic[In, Out]() {
      private val inHandler = new InHandler { override def onPush(): Unit = tracedPush(pushLogic) }

      setHandler(in, inHandler)

      def pullLogic: Unit = if (isClosed(in)) completeStage() else pull(in)
      private val outHandler = new OutHandler { override def onPull(): Unit = pullLogic }
      setHandler(out, outHandler)
    }
}

abstract class TraceStrippingFlowLogic[In, Out](implicit flowShape: FlowShape[(In, TraceContext), Out], moneyExtension: MoneyExtension) extends GraphStageLogic(flowShape) with TracedFlowLogic[In, Out] {
  val in: Inlet[TracedIn] = flowShape.in
  val out: Outlet[Out] = flowShape.out

  /**
   * All Stream Spans are stopped by this function
   *
   * returns Unit
   *
   * Pushes an element down the stream tracing the logic passed to it.
   * All logic to be traced must be passed as stageLogic.
   *
   * @param key        the name of the Span that information will be recorded for
   * @param stageLogic the functionality of this [[akka.stream.scaladsl.Flow]]
   */

  def stopTracePush(key: String, stageLogic: In => (Out, Boolean)): Unit = {
    implicit val (inMessage, traceContext): (In, TraceContext) = grab[TracedIn](in)
    traceContext.tracer.startSpan(key)
    val (outMessage, isSuccessful) = stageLogic(inMessage)
    push[Out](out, outMessage)
    traceContext.tracer.stopSpan(isSuccessful)
    traceContext.tracer.stopSpan(isSuccessful)
  }
}

case class PushLogic[In, Out](key: String, inToOutWithIsSuccessful: In => (Out, Boolean), shouldStop: Boolean = false)
