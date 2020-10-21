package com.comcast.money.core.formatters
import com.comcast.money.api.SpanId
import io.opentelemetry.trace.{TraceFlags, TraceState}

object B3SingleHeaderFormatter extends Formatter {
  private[core] val B3Header = "b3"
  private[core] val B3HeaderPattern = "^((?:[0-9a-f]{16}){1,2})-([0-9a-f]{16})(?:-(1|0|d)(?:-([0-9a-f]{16}))?)?$".r

  override def toHttpHeaders(spanId: SpanId, addHeader: (String, String) => Unit): Unit = {
    val base = f"${spanId.traceIdAsHex}-${spanId.selfIdAsHex}-${if (spanId.isSampled) "1" else "0"}"
    val header = if (spanId.isRoot) {
      base
    } else {
      base + f"-${spanId.parentIdAsHex}"
    }
    addHeader(B3Header, header)
  }

  override def fromHttpHeaders(getHeader: String => String, log: String => Unit): Option[SpanId] = {
    def createRemoteSpan(traceIdHex: String, spanIdHex: String, parentIdHex: Option[String], sampled: Option[String]): Option[SpanId] = {
      val traceId = if (traceIdHex.length == 16) {
        SpanId.parseTraceIdFromHex(("0" * 16) + traceIdHex)
      } else {
        SpanId.parseTraceIdFromHex(traceIdHex)
      }
      val spanId = SpanId.parseIdFromHex(spanIdHex)
      val parentId = parentIdHex.map { SpanId.parseIdFromHex } getOrElse spanId
      val traceFlags = sampled.map { parseSampled } getOrElse TraceFlags.getSampled
      Some(SpanId.createRemote(traceId, parentId, spanId, traceFlags, TraceState.getDefault))
    }

    def parseSampled(sampled: String): Byte = Option(sampled) match {
      case Some("1") | Some("d") | None => TraceFlags.getSampled
      case _ => TraceFlags.getDefault
    }

    def createSampled(sampled: Boolean): Option[SpanId] = {
      val traceId = SpanId.randomTraceId()
      val selfId = SpanId.randomNonZeroLong()
      val traceFlags = if (sampled) TraceFlags.getSampled else TraceFlags.getDefault
      Some(SpanId.createRemote(traceId, selfId, selfId, traceFlags, TraceState.getDefault))
    }

    Option(getHeader(B3Header)) match {
      case Some(B3HeaderPattern(traceId, spanId, sampled, parentId)) =>
        createRemoteSpan(traceId, spanId, Option(parentId), Option(sampled))
      case Some(value) if value.length == 1 => value match {
        case "0" => createSampled(false)
        case "1" | "d" => createSampled(true)
        case _ => None
      }
      case _ => None
    }
  }

  override def fields: Seq[String] = Seq(B3Header)
}
