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

trait TraceStoppingFlow[In, Out] extends GraphStage[FlowShape[(In, TraceContext), Out]] {
  val _inletName: String
  val _outletName: String

  type TracedIn = (In, TraceContext)

  val in: Inlet[TracedIn] = Inlet(name = _inletName)
  val out: Outlet[Out] = Outlet(name = _outletName)

  override def shape: FlowShape[TracedIn, Out] = FlowShape.of(in, out)

  implicit val flowShape: FlowShape[TracedIn, Out] = shape

  def traceStoppingFlowLogic: TraceStoppingFlowLogic[In, Out]

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = traceStoppingFlowLogic
}

object TraceStoppingFlow {
  def apply[In, Out](pushConfig: PushConfig[In, Out], inletName: String, outletName: String): TraceStoppingFlow[In, Out] =
    new TraceStoppingFlow[In, Out] {
      override val _inletName: String = inletName
      override val _outletName: String = outletName

      override def traceStoppingFlowLogic: TraceStoppingFlowLogic[In, Out] = TraceStoppingFlowLogic(pushConfig)
    }

  def apply[In, Out](pushConfig: PushConfig[In, Out]): TraceStoppingFlow[In, Out] =
    apply(pushConfig, inletName = s"${pushConfig.key}Inlet", outletName = s"${pushConfig.key}Outlet")
}

abstract class TraceStoppingFlowLogic[In, Out](implicit flowShape: FlowShape[(In, TraceContext), Out]) extends GraphStageLogic(flowShape) {
  type TracedIn = (In, TraceContext)

  val in: Inlet[TracedIn] = flowShape.in
  val out: Outlet[Out] = flowShape.out

  val inHandler: InHandler

  val outHandler: OutHandler

  def setHandlers: Unit = {
    setHandler(in, inHandler)
    setHandler(out, outHandler)
  }

  /**
   * All Stream Spans are stopped by this function
   *
   * returns Unit
   *
   * Pushes an element down the stream tracing the logic passed to it.
   */

  def stopTracePush(pushConfig: PushConfig[In, Out]): Unit = {
    val (inMessage, traceContext) = grab[TracedIn](in)

    startTrace(pushConfig.key, traceContext)

    val (unitOrMessage, isSuccessful) = pushConfig.pushLogic(inMessage)

    unitOrMessage match {
      case Right(message) => push[Out](out, message)
      case Left(_) => pull(in)
    }

    endStreamTrace(isSuccessful, traceContext)
  }

  protected def startTrace(key: String, traceContext: TraceContext): Unit = traceContext.tracer.startSpan(key)

  protected def endStreamTrace(isSuccessful: Boolean, traceContext: TraceContext): Unit = {
    traceContext.tracer.stopSpan(isSuccessful)
    traceContext.tracer.stopSpan(isSuccessful)
  }
}

object TraceStoppingFlowLogic {
  def apply[In, Out](pushConfig: PushConfig[In, Out])(implicit flowShape: FlowShape[(In, TraceContext), Out]): TraceStoppingFlowLogic[In, Out] =

    pushConfig.tracingDSLUsage match {
      case NotUsingTracingDSL =>
        new TraceStoppingFlowLogic[In, Out]() {
          override val inHandler: InHandler = new InHandler {
            override def onPush(): Unit = stopTracePush(pushConfig)
          }

          def pullLogic: Unit = if (isClosed(in)) completeStage() else pull(in)

          override val outHandler: OutHandler = new OutHandler {
            override def onPull(): Unit = pullLogic
          }

          setHandlers
        }

      case UsingTracingDSL => throw UnsafeSpanStoppingException("Unsafe usage of TraceStoppingFlow spans other than stream spans could be stopped")
    }
}

case class UnsafeSpanStoppingException(msg: String) extends Exception(msg)
