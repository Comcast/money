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

import akka.stream.Attributes.{ AsyncBoundary, Name }
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{ Flow, GraphDSL, Source, Unzip, Zip }
import com.comcast.money.akka.stream.DefaultStreamSpanKeyCreators.DefaultFlowSpanKeyCreator
import com.comcast.money.akka.{ FreshTraceContext, MoneyExtension, SpanContextWithStack, TraceContext }

import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag
import scala.util.Try

/**
 * [[StreamTracingDSL]] are used to trace a stream that has not been built to support tracing
 *
 */

object StreamTracingDSL {

  import DefaultStreamSpanKeyCreators._
  import akka.stream.scaladsl.GraphDSL.Implicits._

  implicit class PortOpsSpanInjector[In: ClassTag](portOps: PortOps[(In, TraceContext)])(implicit builder: Builder[_], fskc: FlowSpanKeyCreator[In] = DefaultFlowSpanKeyCreator[In]) {
    type TracedIn = (In, TraceContext)

    /**
     * Can be used to instrument a generic [[Flow]] that does not accept a SpanContext
     *
     * Example:
     * untraced using standard combinators:
     * {{{
     *  Source(1 to 3) ~> Flow[Int]
     * }}}
     * traced using money combinators:
     * {{{
     * Source(1 to 3) ~|> Flow[Int]
     * }}}
     *
     * @param flow Untraced Flow to be traced
     * @tparam Out type of the Output of the flow
     * @return PortOps[(Out, TraceContext)]
     */

    def ~|>[Out: ClassTag](flow: Flow[In, Out, _]): PortOps[(Out, TraceContext)] = injectSpanInFlow(portOps, flow)

    /**
     * Starts a span for a inlet that is already traced
     *
     * Example:
     *
     * {{{
     * Source(1 to 3) ~|> Flow[Int] ~|> Flow[(Int, TraceContext)].shape.in
     * }}}
     *
     * @param inlet Traced [[Inlet]] that needs a span to be started for it
     * @param iskc  implicit InletSpanKeyCreator to provide a key for the started Span
     * @tparam T is covariant on [[TracedIn]] ie it must be of type (In, TraceContext) or a supertype of the [[Inlet]]
     */

    def ~|>[T >: In: ClassTag](inlet: Inlet[(T, TraceContext)])(implicit iskc: InletSpanKeyCreator[T] = DefaultInletSpanKeyCreator[T]): Unit =
      startSpanForTracedInlet(inlet)

    private def startSpanForTracedInlet[T >: In: ClassTag](inlet: Inlet[(T, TraceContext)])(implicit iskc: InletSpanKeyCreator[T]): Unit =
      portOps.map(stopAndStart(_, iskc.inletToKey(inlet))) ~> inlet

    /**
     * Starts a span for a [[FanInShape]]
     *
     * Example:
     *
     * {{{
     * Source(1 to 3) ~|> bCast
     *
     * bCast.out(0) ~|> Flow[Int] ~<> concat.in(0)
     *
     * bCast.out(1) ~|> Flow[Int] ~<> concat.in(1)
     *
     * concat ~| sink.in
     * }}}
     *
     * @param inlet FanIn inlet to be traced
     * @param fisck implicit [[FanInSpanKeyCreator]] to provide a key for the started Span
     * @tparam T is covariant on [[TracedIn]] ie it must be of type (In, TraceContext) or a supertype of the Inlet
     */

    def ~<>[T >: In: ClassTag](inlet: Inlet[(T, TraceContext)])(implicit fisck: FanInSpanKeyCreator[T] = DefaultFanInSpanKeyCreator[T]): Unit =
      startSpanForTracedFanInInlet(inlet)

    private def startSpanForTracedFanInInlet[T >: In: ClassTag](inlet: Inlet[(T, TraceContext)])(implicit fisck: FanInSpanKeyCreator[T]): Unit =
      portOps.map(stopAndStart(_, fisck.fanInInletToKey(inlet))) ~> inlet

    /**
     * Completes all the Spans in the TraceContext this ends the tracing of the stream
     *
     * Example:
     *
     * {{{
     * Source(List("chunk")) ~|> Flow[String] ~| Flow[String].in
     * }}}
     *
     * @param inlet the inlet of the stream shape that you wish to end the stream with
     * @tparam T must be covariant on [[In]] ie must either be a supertype or the type of the output of the stream
     */

    def ~|[T >: In](inlet: Inlet[T]): Unit = completeTracing(inlet)

    private def completeTracing[T >: In](inlet: Inlet[T]): Unit = portOps.map(doubleStopStrip) ~> inlet

    /**
     * Completes all the remaining spans and allows the stream to continue with regular Akka combinators
     *
     * returns [[PortOps]] of the output of the flow passed in
     *
     * Example 1:
     *
     * {{{
     * val out: PortOps[String] = Source(List("chunk")) ~|> Flow[String] ~|~ Flow[String]
     *
     * SourceShape(out.outlet)
     * }}}
     *
     * Example 2:
     *
     * {{{
     *  Source(List("chunk")) ~|> Flow[String] ~|~ Flow[String] ~> Flow[String].map(someUntracedFunction) ~> Sink.seq[String]
     * }}}
     *
     * @param flow [[Flow]] for the tracing to complete on
     * @tparam Out type of the Output of the [[Flow]]
     * @return PortOps[Out]
     */

    def ~|~[Out: ClassTag](flow: Flow[In, Out, _]): PortOps[Out] = completeTracingAndContinueStream(portOps, flow)

    private def completeTracingAndContinueStream[Out: ClassTag](portOps: PortOps[TracedIn], flow: Flow[In, Out, _]): PortOps[Out] =
      injectSpanInFlow(portOps, flow).map(doubleStopStrip[Out])
  }

  implicit class SourceSpanInjector[In: ClassTag](source: Source[In, _])(implicit
    builder: Builder[_],
      sskc: SourceSpanKeyCreator[In] = DefaultSourceSpanKeyCreator[In],
      fskc: FlowSpanKeyCreator[In] = DefaultFlowSpanKeyCreator[In],
      moneyExtension: MoneyExtension,
      spanContext: SpanContextWithStack = new SpanContextWithStack) {
    type TracedIn = (In, TraceContext)

    /**
     * Starts a TraceContext and injects the SpanContext in to the passed Flow
     * essentially begins the tracing process for a stream
     *
     * Example:
     *
     * {{{
     * Source(List("chunk")) ~|> Flow[String]
     * }}}
     *
     * @param flow first [[Flow]] of the stream that the source connects
     * @tparam Out type of the Output of the [[Flow]]
     * @return PortOps[(Out, TraceContext)]
     */

    def ~|>[Out: ClassTag](flow: Flow[In, Out, _]): PortOps[(Out, TraceContext)] =
      startSpanContextAndInjectSpan(flow)

    private def startSpanContextAndInjectSpan[Out: ClassTag](flow: Flow[In, Out, _]) =
      startSpanContext.map(startSpan(_, fskc.flowToKey(flow))) ~> injectSpan(flow)

    /**
     * Starts a TraceContext for the stream and connects [[Source]] to a traced [[Flow]]
     *
     * Example:
     *
     * {{{
     * Source(List("chunk")) ~|> Flow[(String, TraceContext)]
     * }}}
     *
     * @param flow is a [[Flow]] that takes [[TracedIn]]
     * @tparam TracedOut sub type of (_, TraceContext) forces flow to be traced all the way through
     *                   the underlying Out type of the flow is inferred
     * @return PortOps[(Out, TraceContext)]
     */

    def ~|>[TracedOut <: (_, TraceContext)](flow: Graph[FlowShape[TracedIn, TracedOut], _]): PortOps[TracedOut] =
      sourceViaTracedFlow(flow)

    private def sourceViaTracedFlow[TracedOut <: (_, TraceContext)](flow: Graph[FlowShape[TracedIn, TracedOut], _]) =
      startSpanContext ~> flow

    /**
     * Starts a TraceContext for the stream. Starts a Span for the [[UniformFanOutShape]]
     * then connects the [[Source]] to the traced [[UniformFanOutShape]]
     *
     * Example:
     *
     * {{{
     * val bCast = builder.addTraced(Broadcast(0, false))
     *
     * Source(1 to 3) ~|> bCast
     * }}}
     *
     * @param fanOut A [[UniformFanOutShape]] that accepts [[TracedIn]] either created with [[TracedBuilder]] or manually
     * @param foskc  implicit [[FanOutSpanKeyCreator]] to provide a key for the started Span
     */

    def ~|>(fanOut: UniformFanOutShape[TracedIn, TracedIn])(implicit foskc: FanOutSpanKeyCreator[In] = DefaultFanOutSpanKeyCreator[In]): Unit =
      sourceViaTracedFanOut(fanOut)

    private def sourceViaTracedFanOut(fanOut: UniformFanOutShape[TracedIn, TracedIn])(implicit foskc: FanOutSpanKeyCreator[In]) =
      startSpanContext.map(startSpan(_, foskc.fanOutToKey(fanOut))) ~> fanOut.in

    /**
     * Starts TraceContext for the source currently wrapped by this implicit class
     * started by attaching the source to Flow that creates a new TraceContext
     * from the existing TraceContext and adds a Span for the stream that will
     * only be closed when the stream is completed
     *
     * @return PortsOps[(In, TraceContext)]
     */

    private def startSpanContext: Source[TracedIn, _] =
      source map {
        output =>
          {
            val traceContext = FreshTraceContext(spanContext)
            traceContext.tracer.startSpan(sskc.sourceToKey(source))
            (output, traceContext)
          }
      }
  }

  implicit class OutletSpanInjector[In: ClassTag](outlet: Outlet[(In, TraceContext)])(implicit builder: Builder[_]) {
    /**
     * Connects an [[Outlet]] to a [[Flow]]. Injects the TraceContext in to the Flow and
     * starts and stops a Span for the Flow
     *
     * {{{
     *   Flow[String].shape.out ~|> Flow[String]
     * }}}
     *
     * @param flow untraced flow that needs to be traced
     * @param fskc implicit [[FlowSpanKeyCreator]] to provide a key for the started Span
     * @tparam Out untraced output of the flow
     * @return PortOps[(Out, TraceContext)]
     */

    def ~|>[Out: ClassTag](flow: Flow[In, Out, _])(implicit fskc: FlowSpanKeyCreator[In] = DefaultFlowSpanKeyCreator[In]): PortOps[(Out, TraceContext)] =
      outletViaSpanInjectedFlow(flow)

    private def outletViaSpanInjectedFlow[Out: ClassTag](flow: Flow[In, Out, _])(implicit fsck: FlowSpanKeyCreator[In]) =
      outlet
        .map(stopAndStart(_, fsck.flowToKey(flow)))
        .via(injectSpan[In, Out](flow))
        .map {
          case (input, traceContext) =>
            traceContext.tracer.stopSpan()
            (input, traceContext)
        }

    /**
     * Completes all the Spans in the TraceContext this ends the tracing of the stream
     *
     * Example:
     *
     * {{{
     * Source(List("chunk")) ~|> Flow[String] ~| Flow[String].in
     * }}}
     *
     * @param inlet the inlet of the stream shape that you wish to end the stream with
     * @tparam T must be covariant on [[In]] ie must either be a supertype or the type of the output of the stream
     */

    def ~|[T >: In](inlet: Inlet[T]): Unit = completeTracing(inlet)

    private def completeTracing[T >: In](inlet: Inlet[T]): Unit =
      outlet.map(doubleStopStrip[T]) ~> inlet
  }

  implicit class UniformFanInConnector[T: ClassTag](fanIn: UniformFanInShape[(T, TraceContext), (T, TraceContext)])(implicit builder: Builder[_]) {
    /**
     * Completes the tracing of the stream
     *
     * {{{
     *   val merge = builder.add(Merge[String](2))
     *   merge.out ~| Sink.ignore[String]
     * }}}
     *
     * @param inlet the inlet of the stream shape that you wish to end the stream with
     * @tparam Out Out must be covariant on [[T]] ie must either be a supertype or the type of the output of the stream stage
     */

    def ~|[Out >: T](inlet: Inlet[Out]): Unit = completeTracing(inlet)

    private def completeTracing[Out >: T](inlet: Inlet[Out]) =
      fanIn.out.map(doubleStopStrip[Out]) ~> inlet
  }

  /**
   * INTERNAL API
   */

  /**
   * Injects a TraceContext in to the flow
   *
   * @param portOps the partially constructed stream
   * @param flow    the untraced flow
   * @param builder the builder to continue stream construction
   * @param fskc    [[FlowSpanKeyCreator]] to provide a Span key for the Flow
   * @tparam In  input type of the flow
   * @tparam Out output type of the flow
   * @return PortOps[(Out, TraceContext)]
   */

  private def injectSpanInFlow[In: ClassTag, Out: ClassTag](portOps: PortOps[(In, TraceContext)], flow: Flow[In, Out, _])(implicit builder: Builder[_], fskc: FlowSpanKeyCreator[In]): PortOps[(Out, TraceContext)] =
    portOps.map(stopAndStart(_, fskc.flowToKey(flow))) ~> injectSpan(flow)

  /**
   * DO NOT USE FOR UNORDERED ASYNC SPANCONTEXT WILL NOT BE GIVEN TO CORRECT ELEMENT
   *
   * Returns a traced [[Flow]]
   *
   * the wrapping Flow uses an [[Unzip]] to pull out the SpanContext for an element
   * the element is then passed to the user private defined flow
   * after it has completed the Zip will merge the SpanContext with the element it arrived with
   *
   * this is guaranteed despite the asynchronous boundary due to the order guarantees given by Akka
   *
   * @param flow    an ordered synchronous or asynchronous [[Flow]] that is uninstrumented
   * @param builder Akka Streams [[Builder]] to construct the stream
   * @param fskc    FlowSpanKeyCreator to give the Span a key
   * @tparam In  the underlying Flow's [[Inlet]] type
   * @tparam Out the underlying Flow's [[Outlet]] type
   * @return Flow[(In, TraceContext), (Out, TraceContext), _]
   */

  private def injectSpan[In: ClassTag, Out: ClassTag](flow: Flow[In, Out, _])(implicit builder: Builder[_], fskc: FlowSpanKeyCreator[In]) =
    Flow fromGraph {
      GraphDSL.create() {
        implicit builder: Builder[_] =>
          import akka.stream.scaladsl.GraphDSL.Implicits._

          val unZip = unzipForMaybeAsyncFlow(flow)
          val zip = builder add Zip[Out, TraceContext]()

          val flowShape = builder.add(flow)

          unZip.out0 ~> flowShape ~> zip.in0
          unZip.out1 ~> zip.in1
          FlowShape(unZip.in, zip.out)
      }
    } withAttributes flow.traversalBuilder.attributes

  /**
   * Returns a [[Builder]] added Unzip
   *
   * Determines whether the [[Unzip]] needs to have an async boundary by checking if the [[Flow]]
   * has any async [[Attributes]].
   *
   * The name of the [[Attributes.Attribute]] is used along with whether or not the attribute list
   * contains an [[AsyncBoundary]]
   *
   * @param flow [[Flow]] to be checked for async [[Attributes]]
   * @param builder [[Builder]] to add the created [[Unzip]] to the graph traversal
   * @tparam In  the underlying Flow's [[Inlet]] type
   * @tparam Out the underlying Flow's [[Outlet]] type
   * @return FanOutShape2[(In, TraceContext), In, TraceContext]
   */

  private def unzipForMaybeAsyncFlow[Out: ClassTag, In: ClassTag](flow: Flow[In, Out, _])(implicit builder: Builder[_]) = {

    val traversalBuilder = flow.traversalBuilder

    val hasAsyncName = {
      Try(traversalBuilder.pendingBuilder.get) map {
        _.attributes.attributeList.foldLeft(false) {
          case (true, _) => true
          case (false, Name(name)) if name.contains("Async") => true
          case (_, _) => false
        }
      } getOrElse false
    }

    val hasAsyncBoundary = traversalBuilder.attributes.attributeList.contains(AsyncBoundary)

    if (hasAsyncBoundary || hasAsyncName) builder add Unzip[In, TraceContext]().async
    else builder add Unzip[In, TraceContext]()
  }

  /**
   * Returns the output element and the spanContext with a started Span
   *
   * @param outputWithSpanContext stream element with it's paired [[TraceContext]]
   * @param key the [[com.comcast.money.api.Span]] key for this section of the Stream
   * @tparam Out the type of the output element
   * @return (Out, TraceContext)
   */

  private def startSpan[Out](outputWithSpanContext: (Out, TraceContext), key: String): (Out, TraceContext) = {
    val (_, traceContext) = outputWithSpanContext

    traceContext.tracer.startSpan(key)

    outputWithSpanContext
  }

  /**
   * Returns the output element after stopping two Spans and stripping the SpanContext
   *
   * @param outputWithSpanContext stream element with it's paired [[TraceContext]]
   * @tparam Out the type of the output element
   * @return Out
   */

  private def doubleStopStrip[Out](outputWithSpanContext: (Out, TraceContext)): Out = {
    val (output, traceContext) = outputWithSpanContext

    traceContext.tracer.stopSpan()
    traceContext.tracer.stopSpan()

    output
  }

  /**
   * Returns the output element and it's paired [[TraceContext]]
   *
   * @param outputWithSpanContext stream element with it's paired [[TraceContext]]
   * @param key the [[com.comcast.money.api.Span]] key for this section of the Stream
   * @tparam Out the type of the output element
   * @return (Out, TraceContext)
   */

  private def stopAndStart[Out](outputWithSpanContext: (Out, TraceContext), key: String): (Out, TraceContext) = {
    val (output, traceContext) = outputWithSpanContext

    traceContext.tracer.stopSpan()
    traceContext.tracer.startSpan(key)

    (output, traceContext)
  }
}
