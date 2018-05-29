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

import akka.stream.scaladsl.Flow
import com.comcast.money.akka.TraceContext
import com.comcast.money.akka.stream.DefaultStreamSpanKeyCreators.DefaultFlowSpanKeyCreator

import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

/**
 * Async Tracing for Unordered Flows
 */

object AsyncUnorderedFlowTracing {

  implicit class TracedFlowOps[In: ClassTag](flow: Flow[In, _, _])(implicit executionContext: ExecutionContext, fskc: FlowSpanKeyCreator[In] = DefaultFlowSpanKeyCreator[In]) {
    type TracedIn = (In, TraceContext)

    /**
     * Applies a traced version of [[Flow.mapAsyncUnordered]] to a generic [[Flow]].
     * Guarantees that elements are executed out of order and are attached to the SpanContext they entered with.
     *
     * NB: It is necessary to use flatMap as map has been found to not be guaranteed to close the Span
     *
     * Example:
     *
     * {{{
     *   Flow[String].tracedMapAsyncUnordered(3)(stringToFuture)
     * }}}
     *
     * @param parallelism the number of concurrent futures
     * @param f           the function to apply
     * @return Flow[TracedIn, TracedOut, _]
     */

    def tracedMapAsyncUnordered[Out: ClassTag](parallelism: Int)(f: In => Future[Out]): Flow[TracedIn, (Out, TraceContext), _] =
      Flow[TracedIn].mapAsyncUnordered[(Out, TraceContext)](parallelism) {
        (tuple: TracedIn) =>
          val (in, traceContext) = tuple
          traceContext.tracer.startSpan(fskc.flowToKey(flow))
          f(in) map {
            out =>
              traceContext.tracer.stopSpan()
              (out, traceContext)
          }
      }
  }

}
