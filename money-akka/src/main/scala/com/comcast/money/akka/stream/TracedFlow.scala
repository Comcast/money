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

import akka.stream.stage.{ GraphStage, GraphStageLogic }
import akka.stream.{ FlowShape, Inlet, Outlet }
import com.comcast.money.akka.{ MoneyExtension, SpanContextWithStack }
import com.comcast.money.core.Tracer

/**
 * TracedFlow is used to create a traced version of a user defined [[akka.stream.scaladsl.Flow]]
 * it lessens the amount of boilerplate required to construct a new [[akka.stream.scaladsl.Flow]]
 *
 * @tparam In  input type of the users Flow
 * @tparam Out output type of the users Flow
 */

trait TracedFlow[In, Out] extends GraphStage[FlowShape[(In, SpanContextWithStack), (Out, SpanContextWithStack)]] {
  val inletName: String
  val outletName: String

  type TracedIn = (In, SpanContextWithStack)
  type TracedOut = (Out, SpanContextWithStack)

  implicit val in: Inlet[TracedIn] = Inlet(name = inletName)
  implicit val out: Outlet[TracedOut] = Outlet(name = outletName)

  override def shape: FlowShape[TracedIn, TracedOut] = FlowShape.of(in, out)

  implicit val flowShape: FlowShape[TracedIn, TracedOut] = shape
}

/**
 * TracedFlowLogic is a wrapper interface to [[GraphStageLogic]] for a [[TracedFlow]]
 * it provides functionality for tracing and executing the Flows logic
 *
 * @param flowShape      the traced shape of the Flow being traced
 * @param moneyExtension [[MoneyExtension]] to provide access to [[com.comcast.money.core.Money]]
 * @tparam In  input type of the users Flow
 * @tparam Out output type of the users Flow
 */

abstract class TracedFlowLogic[In, Out](implicit
  flowShape: FlowShape[(In, SpanContextWithStack), (Out, SpanContextWithStack)],
    moneyExtension: MoneyExtension) extends GraphStageLogic(flowShape) {
  type TracedOut = (Out, SpanContextWithStack)
  type TracedIn = (In, SpanContextWithStack)

  implicit val in: Inlet[TracedIn] = flowShape.in
  implicit val out: Outlet[TracedOut] = flowShape.out

  /**
   * Returns a [[Tracer]]
   *
   * used to start and stop a [[com.comcast.money.api.Span]] currently in the [[SpanContextWithStack]]
   *
   * @param spanContext    current [[SpanContextWithStack]]
   * @param moneyExtension [[MoneyExtension]] to provide access to [[com.comcast.money.core.Money]]
   * @return [[Tracer]] created from current spanContext
   */

  private def tracer(implicit
    spanContext: SpanContextWithStack,
    moneyExtension: MoneyExtension): Tracer =
    moneyExtension.tracer(spanContext)

  /**
   * returns Unit
   *
   * Pushes an element down the stream tracing the logic passed to it.
   * All logic to be traced must be passed as stageLogic
   *
   * @param key the name of the Span that information will be recorded for
   * @param stageLogic the functionality of this [[akka.stream.scaladsl.Flow]]
   * @param isSuccessful whether or not this Flow was successful
   */

  def tracedPush(key: String, stageLogic: In => Out, isSuccessful: Boolean = true): Unit = {
    implicit val (inMessage, spanContext) = grab[TracedIn](in)
    tracer.startSpan(key)
    val outMessage = stageLogic(inMessage)
    push[TracedOut](out, (outMessage, spanContext))
    tracer.stopSpan(isSuccessful)
  }

  /**
   * All Spans are stopped by this function
   *
   * returns Unit
   *
   * Pushes an element down the stream tracing the logic passed to it.
   * All logic to be traced must be passed as stageLogic.
   *
   * @param key the name of the Span that information will be recorded for
   * @param stageLogic the functionality of this [[akka.stream.scaladsl.Flow]]
   * @param isSuccessful whether or not this Stream was successful
   */

  def stopTracePush(key: String, stageLogic: In => Out, isSuccessful: Boolean = true): Unit = {
    implicit val (inMessage, spanContext): (In, SpanContextWithStack) = grab[TracedIn](in)
    tracer.startSpan(key)
    val outMessage = stageLogic(inMessage)
    push[TracedOut](out, (outMessage, spanContext))
    spanContext.getAll.foreach(_ => tracer.stopSpan(isSuccessful))
  }
}
