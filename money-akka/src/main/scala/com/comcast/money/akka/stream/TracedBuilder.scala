package com.comcast.money.akka.stream

import akka.stream.{Graph, UniformFanInShape}
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl.{Concat, Partition}
import com.comcast.money.akka.SpanContextWithStack


trait TracedBuilder {

  implicit class TracedBuilder(builder: Builder[_]) {
    def tracedAdd[T](partition: Partition[T]) =
      builder.add(
        graph = Partition[(T, SpanContextWithStack)](
          outputPorts = partition.outputPorts,
          partitioner = (tWithSpan: (T, SpanContextWithStack)) => partition.partitioner(tWithSpan._1)
        )
      )

    def tracedConcat[T](concat: Graph[UniformFanInShape[T, T], _]) =
      builder.add(
        graph = Concat[(T, SpanContextWithStack)](concat.shape.n)
      )
  }

}
