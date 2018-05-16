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

package com.comcast.money.akka

import akka.Done
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.GraphDSL.Implicits.PortOps
import akka.stream.scaladsl.{ Flow, GraphDSL, Partition, RunnableGraph, Sink, Source }
import akka.stream.stage.GraphStage
import com.comcast.money.akka.stream.AsyncUnorderedFlowTracing.TracedFlowOps
import com.comcast.money.akka.stream.DefaultStreamSpanKeyCreators._
import com.comcast.money.akka.stream._

import scala.concurrent.{ ExecutionContext, Future }

object TestStreamConstants {
  def replicateAndAppend[T](seq: Seq[T], numberOfreplicas: Int = 2): Seq[T] =
    (1 to numberOfreplicas).map(_ => seq).reduce(_ ++ _)

  val stream = "Stream"
  val stringToString = "StringToString"
  val fanInOfString = "FanInOfString"
  val fanOutOfString = "FanOutOfString"
}

class TestStreams(implicit moneyExtension: MoneyExtension) {

  import StreamTracingDSL._
  import TracedBuilder._

  private val sink = Sink.ignore

  private def source = Source(List("chunk"))

  def simple(implicit
    fskc: FlowSpanKeyCreator[String] = DefaultFlowSpanKeyCreator[String],
    sskc: SourceSpanKeyCreator[String] = DefaultSourceSpanKeyCreator[String]) =
    RunnableGraph fromGraph {
      GraphDSL.create(sink) { implicit builder: Builder[Future[Done]] => sink =>
        source ~|> Flow[String] ~| sink.in

        ClosedShape
      }
    }

  def simpleTakingSpanContext(implicit
    spanContextWithStack: SpanContextWithStack,
    fskc: FlowSpanKeyCreator[String] = DefaultFlowSpanKeyCreator[String],
    sskc: SourceSpanKeyCreator[String] = DefaultSourceSpanKeyCreator[String]) =
    RunnableGraph fromGraph {
      GraphDSL.create(sink) { implicit builder: Builder[Future[Done]] => sink =>
        source ~|> Flow[String] ~| sink.in

        ClosedShape
      }
    }

  def simpleWithInlets(implicit inletSpanKeyCreator: InletSpanKeyCreator[String] = DefaultInletSpanKeyCreator[String]) =
    RunnableGraph fromGraph {
      GraphDSL.create(sink) { implicit builder: Builder[Future[Done]] => sink =>
        val flowShape = builder.add(Flow[(String, SpanContextWithStack)])
        source ~|> Flow[String] ~|> flowShape.in
        flowShape.out ~| sink.in

        ClosedShape
      }
    }

  def sourceEndingWithFlow =
    Source fromGraph {
      GraphDSL.create() {
        implicit builder =>
          val out: PortOps[String] = source ~|> Flow[String] ~|~ Flow[String]

          SourceShape(out.outlet)
      }
    }

  val partitioner: String => Int = {
    case "chunk" => 0
    case "funk" => 1
  }

  def fanOutFanIn(
    fanInCreator: TracedBuilder => UniformFanInShape[(String, SpanContextWithStack), (String, SpanContextWithStack)] = _.tracedConcat(),
    fanOutRaw: GraphStage[UniformFanOutShape[String, String]] = Partition[String](2, partitioner)
  )(implicit
    fisck: FanInSpanKeyCreator[String] = DefaultFanInSpanKeyCreator[String],
    fosck: FanOutSpanKeyCreator[String] = DefaultFanOutSpanKeyCreator[String]) =

    RunnableGraph fromGraph {
      GraphDSL.create(sink) { implicit builder: Builder[Future[Done]] => sink =>

        val fanOut = builder.tracedAdd(fanOutRaw)

        val fanIn: UniformFanInShape[(String, SpanContextWithStack), (String, SpanContextWithStack)] = fanInCreator(builder)

        Source(List("chunk", "funk")) ~|> fanOut

        fanOut.out(0) ~|> Flow[String] ~<> fanIn.in(0)

        fanOut.out(1) ~|> Flow[String] ~<> fanIn.in(1)

        fanIn ~| sink.in

        ClosedShape
      }
    }

  private def stringToFuture(sleeps: (Long, Long))(implicit executionContext: ExecutionContext) =
    (string: String) =>
      Future {
        string.last.asDigit match {
          case 2 => Thread.sleep(sleeps._1)
          case 3 => Thread.sleep(sleeps._2)
          case _ =>
        }
        string
      }

  private def accumulatedStringToFuture(implicit executionContext: ExecutionContext) =
    (acc: String, string: String) => stringToFuture((400L, 400L))(executionContext)(string).map(acc + _.last)

  type TracedString = (String, SpanContextWithStack)

  def asyncOutOfOrder(implicit executionContext: ExecutionContext) = asyncStream(_ => Right(Flow[String].tracedMapAsyncUnordered(3)(stringToFuture((400L, 200L)))))

  def asyncSimple(implicit executionContext: ExecutionContext) = asyncStream(_ => Left(Flow[String].mapAsync(3)(stringToFuture((400L, 400L)))))

  def asyncScan(implicit executionContext: ExecutionContext) = asyncStream(_ => Left(Flow[String].scanAsync("")(accumulatedStringToFuture)))

  def asyncManyElements(implicit executionContext: ExecutionContext) =
    Source fromGraph {
      GraphDSL.create() {
        implicit builder =>
          val iterator = (1 to 100).map(i => s"chunk$i").iterator

          val out = Source.fromIterator(() => iterator) ~|> Flow[String].mapAsync(30)(stringToFuture((60, 60))) ~|~ Flow[String].map(ChunkStreamPart(_))

          SourceShape(out.outlet)
      }
    }

  def asyncStream(asyncFlowCreator: TracedBuilder => Either[Flow[String, String, _], Flow[TracedString, TracedString, _]])(implicit executionContext: ExecutionContext) =
    RunnableGraph fromGraph {
      GraphDSL.create(Sink.seq[String]) { implicit builder: Builder[Future[Seq[String]]] => sink =>
        val iterator = List("chunk1", "chunk2", "chunk3").iterator
        asyncFlowCreator(builder) fold (
          asyncFlow => Source.fromIterator(() => iterator) ~|> asyncFlow ~| sink.in,
          asyncUnorderedFlow => Source.fromIterator(() => iterator) ~|> asyncUnorderedFlow ~| sink.in
        )

        ClosedShape
      }
    }

  def namedFlow(implicit fskc: FlowSpanKeyCreator[String] = DefaultFlowSpanKeyCreator[String]) =
    RunnableGraph fromGraph {
      GraphDSL.create(sink) { implicit builder: Builder[Future[Done]] => sink =>
        source ~|> Flow[String].addAttributes(Attributes(Attributes.Name("SomeFlowName"))) ~| sink.in

        ClosedShape
      }
    }
}
