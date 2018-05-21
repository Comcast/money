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
import com.comcast.money.akka.TraceContext

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

  def tracedFlowLogic: TracedFlowLogic[In, Out]

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = tracedFlowLogic
}

object TracedFlow {
  def apply[In, Out](pushConfig: PushConfig[In, Out], inletName: String, outletName: String): TracedFlow[In, Out] =
    new TracedFlow[In, Out] {
      override val _inletName: String = inletName
      override val _outletName: String = outletName
      override def tracedFlowLogic: TracedFlowLogic[In, Out] = TracedFlowLogic(pushConfig)
    }

  def apply[In, Out](pushConfig: PushConfig[In, Out]): TracedFlow[In, Out] = apply(pushConfig, inletName = s"${pushConfig.key}Inlet", outletName = s"${pushConfig.key}Outlet")
}

/**
 * TracedRetainingFlowLogic is a wrapper interface to [[GraphStageLogic]] for a [[TracedFlow]]
 * it provides functionality for tracing and executing the Flows logic
 *
 * @param flowShape the traced shape of the Flow being traced
 * @tparam In  input type of the users Flow
 * @tparam Out output type of the users Flow
 */

abstract class TracedFlowLogic[In, Out](implicit flowShape: FlowShape[(In, TraceContext), (Out, TraceContext)]) extends GraphStageLogic(flowShape) {
  type TracedIn = (In, TraceContext)
  type TracedOut = (Out, TraceContext)

  val in: Inlet[TracedIn] = flowShape.in
  val out: Outlet[TracedOut] = flowShape.out

  val inHandler: InHandler

  val outHandler: OutHandler

  def setHandlers: Unit = {
    setHandler(in, inHandler)
    setHandler(out, outHandler)
  }

  /**
   * returns Unit
   *
   * Pushes an element down the stream tracing the logic passed to it.
   * All logic to be traced must be passed as stageLogic
   *
   * @param pushConfig contains the [[com.comcast.money.api.Span]] key and logic to be applied to each element
   */

  protected def tracedPush(pushConfig: PushConfig[In, Out]): Unit = {

    val (inMessage, traceContext) = grab[TracedIn](in)

    startTrace(pushConfig.key, traceContext)

    val (unitOrMessage, isSuccessful) = pushConfig.pushLogic(inMessage)

    unitOrMessage match {
      case Right(message) => push[TracedOut](out, (message, traceContext))
      case Left(_) => pull(in)
    }

    if (pushConfig.tracingDSLUsage == NotUsingTracingDSL) endTrace(isSuccessful, traceContext)
  }

  protected def startTrace(key: String, traceContext: TraceContext): Unit = traceContext.tracer.startSpan(key)

  protected def endTrace(isSuccessful: Boolean, traceContext: TraceContext): Unit = traceContext.tracer.stopSpan(isSuccessful)
}

object TracedFlowLogic {
  def apply[In, Out](pushConfig: PushConfig[In, Out])(implicit flowShape: FlowShape[(In, TraceContext), (Out, TraceContext)]): TracedFlowLogic[In, Out] =
    new TracedFlowLogic[In, Out]() {
      override val inHandler: InHandler = new InHandler { override def onPush(): Unit = tracedPush(pushConfig) }

      def pullLogic: Unit = if (isClosed(in)) completeStage() else pull(in)
      override val outHandler: OutHandler = new OutHandler { override def onPull(): Unit = pullLogic }

      setHandlers
    }
}
