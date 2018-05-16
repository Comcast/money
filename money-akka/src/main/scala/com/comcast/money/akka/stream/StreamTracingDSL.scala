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
import com.comcast.money.akka.{ MoneyExtension, SpanContextWithStack }

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

  implicit class PortOpsSpanInjector[In: ClassTag](portOps: PortOps[(In, SpanContextWithStack)])(implicit
    builder: Builder[_],
      moneyExtension: MoneyExtension,
      fskc: FlowSpanKeyCreator[In] = DefaultFlowSpanKeyCreator[In]) {
    type TracedIn = (In, SpanContextWithStack)

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
     * @return PortOps[(Out, SpanContextWithStack)]
     */

    def ~|>[Out: ClassTag](flow: Flow[In, Out, _]): PortOps[(Out, SpanContextWithStack)] = injectSpanInFlow(portOps, flow)

    /**
     * Starts a span for a inlet that is already traced
     *
     * Example:
     *
     * {{{
     * Source(1 to 3) ~|> Flow[Int] ~|> Flow[(Int, SpanContextWithStack)].shape.in
     * }}}
     *
     * @param inlet Traced [[Inlet]] that needs a span to be started for it
     * @param iskc  implicit InletSpanKeyCreator to provide a key for the started Span
     * @tparam T is covariant on [[TracedIn]] ie it must be of type (In, SpanContextWithStack) or a supertype of the [[Inlet]]
     */

    def ~|>[T >: In: ClassTag](inlet: Inlet[(T, SpanContextWithStack)])(implicit iskc: InletSpanKeyCreator[T] = DefaultInletSpanKeyCreator[T]): Unit =
      startSpanForTracedInlet(inlet)

    private def startSpanForTracedInlet[T >: In: ClassTag](inlet: Inlet[(T, SpanContextWithStack)])(implicit iskc: InletSpanKeyCreator[T]): Unit =
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
     * @tparam T is covariant on [[TracedIn]] ie it must be of type (In, SpanContextWithStack) or a supertype of the Inlet
     */

    def ~<>[T >: In: ClassTag](inlet: Inlet[(T, SpanContextWithStack)])(implicit fisck: FanInSpanKeyCreator[T] = DefaultFanInSpanKeyCreator[T]): Unit =
      startSpanForTracedFanInInlet(inlet)

    private def startSpanForTracedFanInInlet[T >: In: ClassTag](inlet: Inlet[(T, SpanContextWithStack)])(implicit fisck: FanInSpanKeyCreator[T]): Unit =
      portOps.map(stopAndStart(_, fisck.fanInInletToKey(inlet))) ~> inlet

    /**
     * Completes all the Spans in the SpanContextWithStack this ends the tracing of the stream
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
                                                                         moneyExtension: MoneyExtension,
                                                                         sskc: SourceSpanKeyCreator[In] = DefaultSourceSpanKeyCreator[In],
                                                                         fskc: FlowSpanKeyCreator[In] = DefaultFlowSpanKeyCreator[In],
                                                                         spanContext: SpanContextWithStack = new SpanContextWithStack) {
    type TracedIn = (In, SpanContextWithStack)

    /**
     * Starts a SpanContextWithStack and injects the SpanContext in to the passed Flow
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
     * @return PortOps[(Out, SpanContextWithStack)]
     */

    def ~|>[Out: ClassTag](flow: Flow[In, Out, _]): PortOps[(Out, SpanContextWithStack)] =
      startSpanContextAndInjectSpan(flow)

    private def startSpanContextAndInjectSpan[Out: ClassTag](flow: Flow[In, Out, _]) =
      startSpanContext.map(startSpan(_, fskc.flowToKey(flow))) ~> injectSpan(flow)

    /**
     * Starts a SpanContextWithStack for the stream and connects [[Source]] to a traced [[Flow]]
     *
     * Example:
     *
     * {{{
     * Source(List("chunk")) ~|> Flow[(String, SpanContextWithStack)]
     * }}}
     *
     * @param flow is a [[Flow]] that takes [[TracedIn]]
     * @tparam TracedOut sub type of (_, SpanContextWithStack) forces flow to be traced all the way through
     *                   the underlying Out type of the flow is inferred
     * @return PortOps[(Out, SpanContextWithStack)]
     */

    def ~|>[TracedOut <: (_, SpanContextWithStack)](flow: Graph[FlowShape[TracedIn, TracedOut], _]): PortOps[TracedOut] =
      sourceViaTracedFlow(flow)

    private def sourceViaTracedFlow[TracedOut <: (_, SpanContextWithStack)](flow: Graph[FlowShape[TracedIn, TracedOut], _]) =
      startSpanContext ~> flow

    /**
     * Starts a SpanContextWithStack for the stream. Starts a Span for the [[UniformFanOutShape]]
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
     * Starts SpanContextWithStack for the source currently wrapped by this implicit class
     * started by attaching the source to Flow that creates a new SpanContextWithStack
     * from the existing SpanContextWithStack and adds a Span for the stream that will
     * only be closed when the stream is completed
     *
     * @return PortsOps[(In, SpanContextWithStack)]
     */

    private def startSpanContext(implicit moneyExtension: MoneyExtension): Source[TracedIn, _] =
      source.map {
        input =>
          val freshSpanContext = spanContext.copy
          moneyExtension.tracer(freshSpanContext).startSpan(sskc.sourceToKey(source))
          (input, freshSpanContext)
      }
  }

  implicit class OutletSpanInjector[In: ClassTag](outlet: Outlet[(In, SpanContextWithStack)])(implicit builder: Builder[_]) {
    /**
     * Connects an [[Outlet]] to a [[Flow]]. Injects the SpanContextWithStack in to the Flow and
     * starts and stops a Span for the Flow
     *
     * {{{
     *   Flow[String].shape.out ~|> Flow[String]
     * }}}
     *
     * @param flow untraced flow that needs to be traced
     * @param fskc implicit [[FlowSpanKeyCreator]] to provide a key for the started Span
     * @tparam Out untraced output of the flow
     * @return PortOps[(Out, SpanContextWithStack)]
     */

    def ~|>[Out: ClassTag](flow: Flow[In, Out, _])(implicit
      fskc: FlowSpanKeyCreator[In] = DefaultFlowSpanKeyCreator[In],
      moneyExtension: MoneyExtension): PortOps[(Out, SpanContextWithStack)] =
      outletViaSpanInjectedFlow(flow)

    private def outletViaSpanInjectedFlow[Out: ClassTag](flow: Flow[In, Out, _])(implicit
      fsck: FlowSpanKeyCreator[In],
      moneyExtension: MoneyExtension) =
      outlet
        .map(stopAndStart(_, fsck.flowToKey(flow)))
        .via(injectSpan[In, Out](flow))
        .map {
          case (input, spanContext) =>
            moneyExtension.tracer(spanContext).stopSpan()
            (input, spanContext)
        }

    /**
     * Completes all the Spans in the SpanContextWithStack this ends the tracing of the stream
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

    def ~|[T >: In](inlet: Inlet[T])(implicit moneyExtension: MoneyExtension): Unit = completeTracing(inlet)

    private def completeTracing[T >: In](inlet: Inlet[T])(implicit moneyExtension: MoneyExtension): Unit =
      outlet.map(doubleStopStrip[T]) ~> inlet
  }

  implicit class UniformFanInConnector[T: ClassTag](fanIn: UniformFanInShape[(T, SpanContextWithStack), (T, SpanContextWithStack)])(implicit
    builder: Builder[_],
      moneyExtension: MoneyExtension) {
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

    private def completeTracing[Out >: T](inlet: Inlet[Out])(implicit moneyExtension: MoneyExtension) =
      fanIn.out.map(doubleStopStrip[Out]) ~> inlet
  }

  /**
   * INTERNAL API
   */

  /**
   * Injects a SpanContextWithStack in to the flow
   *
   * @param portOps the partially constructed stream
   * @param flow    the untraced flow
   * @param builder the builder to continue stream construction
   * @param fskc    [[FlowSpanKeyCreator]] to provide a Span key for the Flow
   * @tparam In  input type of the flow
   * @tparam Out output type of the flow
   * @return PortOps[(Out, SpanContextWithStack)]
   */

  private def injectSpanInFlow[In: ClassTag, Out: ClassTag](
    portOps: PortOps[(In, SpanContextWithStack)],
    flow: Flow[In, Out, _]
  )(implicit
    builder: Builder[_],
    moneyExtension: MoneyExtension,
    fskc: FlowSpanKeyCreator[In]): PortOps[(Out, SpanContextWithStack)] =
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
   * @return Flow[(In, SpanContextWithStack), (Out, SpanContextWithStack), _]
   */

  private def injectSpan[In: ClassTag, Out: ClassTag](flow: Flow[In, Out, _])(implicit
    builder: Builder[_],
    moneyExtension: MoneyExtension,
    fskc: FlowSpanKeyCreator[In]) =
    Flow fromGraph {
      GraphDSL.create() {
        implicit builder: Builder[_] =>
          import akka.stream.scaladsl.GraphDSL.Implicits._

          val unZip = unzipForFlow(flow)
          val zip = builder add Zip[Out, SpanContextWithStack]()

          val flowShape = builder.add(flow)

          unZip.out0 ~> flowShape ~> zip.in0
          unZip.out1 ~> zip.in1
          FlowShape(unZip.in, zip.out)
      }
    } withAttributes flow.traversalBuilder.attributes

  /**
    * Returns a Unzip
    *
    * Determines by checking for a A
    *
    * @param flow
    * @param builder
    * @tparam Out
    * @tparam In
    * @return
    */

  private def unzipForFlow[Out: ClassTag, In: ClassTag](flow: Flow[In, Out, _])(implicit builder: Builder[_]) = {

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

    if (hasAsyncBoundary || hasAsyncName) builder add Unzip[In, SpanContextWithStack]().async
    else builder add Unzip[In, SpanContextWithStack]()
  }

  /**
   * Returns the output element and the spanContext with a started Span
   *
   * @param outputWithSpanContext stream element with it's paired [[SpanContextWithStack]]
   * @param key the [[com.comcast.money.api.Span]] key for this section of the Stream
   * @param moneyExtension the [[MoneyExtension]] attached to this [[akka.actor.ActorSystem]]
   * @tparam Out the type of the output element
   * @return (Out, SpanContextWithStack)
   */

  private def startSpan[Out](outputWithSpanContext: (Out, SpanContextWithStack), key: String)(implicit moneyExtension: MoneyExtension): (Out, SpanContextWithStack) = {
    val (_, spanContext) = outputWithSpanContext

    moneyExtension.tracer(spanContext).startSpan(key)

    outputWithSpanContext
  }

  /**
   * Returns the output element after stopping two Spans and stripping the SpanContext
   *
   * @param outputWithSpanContext stream element with it's paired [[SpanContextWithStack]]
   * @param moneyExtension the [[MoneyExtension]] attached to this [[akka.actor.ActorSystem]]
   * @tparam Out the type of the output element
   * @return Out
   */

  private def doubleStopStrip[Out](outputWithSpanContext: (Out, SpanContextWithStack))(implicit moneyExtension: MoneyExtension): Out = {
    val (output, spanContext) = outputWithSpanContext
    val tracer = moneyExtension.tracer(spanContext)

    tracer.stopSpan()
    tracer.stopSpan()

    output
  }

  /**
   * Returns the output element and it's paired [[SpanContextWithStack]]
   *
   * @param outputWithSpanContext stream element with it's paired [[SpanContextWithStack]]
   * @param key the [[com.comcast.money.api.Span]] key for this section of the Stream
   * @param moneyExtension the [[MoneyExtension]] attached to this [[akka.actor.ActorSystem]]
   * @tparam Out the type of the output element
   * @return (Out, SpanContextWithStack)
   */

  private def stopAndStart[Out](outputWithSpanContext: (Out, SpanContextWithStack), key: String)(implicit moneyExtension: MoneyExtension): (Out, SpanContextWithStack) = {
    val (output, spanContext) = outputWithSpanContext
    val tracer = moneyExtension.tracer(spanContext)

    tracer.stopSpan()
    tracer.startSpan(key)

    (output, spanContext)
  }
}

/**
 * Async Tracing for Unordered Flows
 */

object AsyncUnorderedFlowTracing {

  implicit class TracedFlowOps[In: ClassTag, Out: ClassTag](flow: Flow[In, Out, _])(implicit
    moneyExtension: MoneyExtension,
      executionContext: ExecutionContext,
      fskc: FlowSpanKeyCreator[In] = DefaultFlowSpanKeyCreator[In]) {
    type TracedIn = (In, SpanContextWithStack)
    type TracedOut = (Out, SpanContextWithStack)

    /**
     * Applies a traced version of [[Flow.mapAsyncUnordered]] to a generic [[Flow]].
     * Guarantees that elements are executed out of order and are attached to the SpanContext they entered with.
     *
     * NB: It is necessary to use flatMap as map has been found to not be guaranteed to close the Span
     *
     * Example:
     *
     * Flow[String].tracedMapAsyncUnordered(3)(stringToFuture)
     *
     * @param parallelism the number of concurrent futures
     * @param f           the function to apply
     * @return Flow[TracedIn, TracedOut, _]
     */

    def tracedMapAsyncUnordered(parallelism: Int)(f: In => Future[Out]): Flow[TracedIn, TracedOut, _] =
      Flow[TracedIn].mapAsyncUnordered[TracedOut](parallelism) {
        (tuple: TracedIn) =>
          val (in, spanContext) = tuple
          moneyExtension.tracer(spanContext).startSpan(fskc.flowToKey(flow))
          f(in) map {
            out =>
              moneyExtension.tracer(spanContext).stopSpan()
              (out, spanContext)
          }
      }
  }

}
