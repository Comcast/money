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

package com.comcast.money.akka.stream.dsl

import akka.stream._
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{ Balance, Broadcast, Concat, Interleave, Merge, MergePrioritized, Partition }
import akka.stream.stage.GraphStage
import com.comcast.money.akka.TraceContext

object TracedBuilder {

  /**
   * TracedBuilder provides a way to take a constructed shape
   * and convert it to a traced version of that shape
   *
   * Primarily designed as a way to painlessly incorporate generic structures like [[akka.stream.scaladsl.Broadcast]]
   * or [[akka.stream.scaladsl.Merge]] particularly for FanOutShapes and FanInShapes as these are not currently
   * traceable with just the [[StreamTracingDSL]]
   *
   * @param builder [[Builder]] wrapped by the implicit TracedBuilder class
   */

  implicit class TracedBuilder(builder: Builder[_]) {

    /**
     * UNZIP IS NOT SUPPORTED
     *
     * Returns a traced version of the supported [[UniformFanOutShape]]
     *
     * @param fanOutShape supported shape to be converted to
     * @tparam T type of underlying stream elements
     * @return UniformFanOutShape[(T, TraceContext), (T, TraceContext)]
     * @throws UnsupportedUniformFanOutShape if a FanOut shape that is not Partition, Broadcast or Balance is passed
     */

    def tracedAdd[T](fanOutShape: GraphStage[UniformFanOutShape[T, T]]): UniformFanOutShape[(T, TraceContext), (T, TraceContext)] = {
      type TracedT = (T, TraceContext)

      fanOutShape match {
        case partition: Partition[T] =>
          builder add Partition[TracedT](partition.outputPorts, partitioner = (tWithSpan: TracedT) => partition.partitioner(tWithSpan._1))

        case broadcast: Broadcast[T] => builder add Broadcast[TracedT](broadcast.outputPorts, broadcast.eagerCancel)

        case balance: Balance[T] => builder add Balance[TracedT](balance.outputPorts, balance.waitForAllDownstreams)

        case unsupportedShape =>
          throw UnsupportedUniformFanOutShape(
            s"Attempted tracedAdd of unsupported UniformFanOutShape: $unsupportedShape.\n" +
              s"Supported shapes are Partition, Broadcast and Balance"
          )
      }
    }

    def tracedAdd[T](fanIn: GraphStage[UniformFanInShape[T, T]]): UniformFanInShape[(T, TraceContext), (T, TraceContext)] = {
      type TracedT = (T, TraceContext)

      fanIn match {
        case merge: Merge[T] => builder add Merge[TracedT](merge.inputPorts, merge.eagerComplete)

        case mergePrioritised: MergePrioritized[T] => builder add MergePrioritized[TracedT](mergePrioritised.priorities, mergePrioritised.eagerComplete)

        case unsupportedShape =>
          throw UnsupportedUniformFanInShape(
            s"Attempted tracedAdd of unsupported UniformFanInShape: $unsupportedShape.\n" +
              s"Supported shapes are Merge, Interleave and MergePrioritised"
          )
      }
    }

    /**
     * creates a [[Interleave]] and adds it to the builder
     *
     * NB this is an edge case due to the fact that [[Interleave.apply]] creates
     * a [[ Graph[UniformFanInShape] ]] not a [[Interleave]]
     *
     * @param inputPorts number of inlets
     * @param segmentSize number of elements to send downstream before switching to next input port
     * @param eagerClose if true, interleave completes upstream if any of its upstream completes.
     * @tparam T type of underlying stream elements
     * @return UniformFanInShape[(T, TraceContext), (T, TraceContext)]
     */

    def tracedInterleave[T](inputPorts: Int, segmentSize: Int, eagerClose: Boolean = false) =
      builder add Interleave[(T, TraceContext)](inputPorts, segmentSize, eagerClose)

    /**
     * creates a [[Concat]] and adds it to the builder
     *
     * NB this is an edge case due to the fact that [[Concat.apply]] creates
     * a [[ Graph[UniformFanInShape] ]] not a Concat
     *
     * @param inputPorts number of inlets
     * @tparam T type of underlying stream elements
     * @return UniformFanInShape[(T, TraceContext), (T, TraceContext)]
     */

    def tracedConcat[T](inputPorts: Int = 2) = builder add Concat[(T, TraceContext)](inputPorts)
  }

}

case class UnsupportedUniformFanOutShape(msg: String) extends Throwable(msg)

case class UnsupportedUniformFanInShape(msg: String) extends Throwable(msg)
