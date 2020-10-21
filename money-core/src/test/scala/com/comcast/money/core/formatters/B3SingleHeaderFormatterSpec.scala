package com.comcast.money.core.formatters

import java.util.UUID

import com.comcast.money.api.SpanId
import com.comcast.money.core.TraceGenerators
import com.comcast.money.core.formatters.B3SingleHeaderFormatter.B3Header
import com.comcast.money.core.formatters.FormatterUtils._
import io.opentelemetry.trace.{TraceFlags, TraceState}
import org.mockito.Mockito.{verify, verifyNoMoreInteractions}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class B3SingleHeaderFormatterSpec extends AnyWordSpec with MockitoSugar with Matchers with ScalaCheckDrivenPropertyChecks with TraceGenerators {
  "B3SingleHeaderFormatter" should {
    "read B3 header correctly for any valid hex encoded header" in {
      forAll { (traceIdValue: UUID, parentSpanIdValue: Long, spanIdValue: Long, sampled: Boolean, debug: Boolean) =>
        whenever(isValidIds(traceIdValue, parentSpanIdValue, spanIdValue)) {
          val traceFlags = if (sampled | debug) TraceFlags.getSampled else TraceFlags.getDefault
          val expectedSpanId = SpanId.createRemote(traceIdValue.toString, parentSpanIdValue, spanIdValue, traceFlags, TraceState.getDefault)
          val spanId = B3SingleHeaderFormatter.fromHttpHeaders(
            getHeader = {
              case B3Header => f"${traceIdValue.hex64or128}-${spanIdValue.hex64}-${sampledFlag(sampled, debug)}-${parentSpanIdValue.hex64}"
            })
          spanId shouldBe Some(expectedSpanId)
        }
      }
    }

    "read B3 header correctly for any valid hex encoded header without a parent ID" in {
      forAll { (traceIdValue: UUID, spanIdValue: Long, sampled: Boolean, debug: Boolean) =>
        whenever(isValidIds(traceIdValue, spanIdValue)) {
          val traceFlags = if (sampled | debug) TraceFlags.getSampled else TraceFlags.getDefault
          val expectedSpanId = SpanId.createRemote(traceIdValue.toString, spanIdValue, spanIdValue, traceFlags, TraceState.getDefault)
          val spanId = B3SingleHeaderFormatter.fromHttpHeaders(
            getHeader = {
              case B3Header => f"${traceIdValue.hex64or128}-${spanIdValue.hex64}-${sampledFlag(sampled, debug)}"
            })
          spanId shouldBe Some(expectedSpanId)
        }
      }
    }

    "read B3 header correctly for any valid hex encoded header without a parent ID or sampled" in {
      forAll { (traceIdValue: UUID, spanIdValue: Long) =>
        whenever(isValidIds(traceIdValue, spanIdValue)) {
          val expectedSpanId = SpanId.createRemote(traceIdValue.toString, spanIdValue, spanIdValue, TraceFlags.getSampled, TraceState.getDefault)
          val spanId = B3SingleHeaderFormatter.fromHttpHeaders(
            getHeader = {
              case B3Header => f"${traceIdValue.hex64or128}-${spanIdValue.hex64}"
            })
          spanId shouldBe Some(expectedSpanId)
        }
      }
    }

    "read B3 header correctly with only sampled" in {
      forAll { (sampled: Boolean, debug: Boolean) =>
        val spanId = B3SingleHeaderFormatter.fromHttpHeaders(
          getHeader = {
            case B3Header => sampledFlag(sampled, debug)
          })
        spanId should not be None
        spanId.get.isSampled shouldBe (sampled | debug)
      }
    }

    "fail to read B3 headers correctly for invalid headers" in {
      val spanId = B3SingleHeaderFormatter.fromHttpHeaders(getHeader = _ => "garbage")
      spanId shouldBe None
    }

    "create B3 header correctly given any valid character UUID for trace-Id and any valid long integers for parent and span ID, where if parent == span id, parent will not be emitted" in {
      forAll { (traceIdValue: UUID, parentSpanIdValue: Long, spanIdValue: Long, sampled: Boolean) =>
        whenever(isValidIds(traceIdValue, parentSpanIdValue, spanIdValue)) {

          val traceFlags = if (sampled) TraceFlags.getSampled else TraceFlags.getDefault
          val expectedSpanId = SpanId.createRemote(traceIdValue.toString, parentSpanIdValue, spanIdValue, traceFlags, TraceState.getDefault)
          B3SingleHeaderFormatter.toHttpHeaders(expectedSpanId, (k, v) => k match {
            case B3Header if expectedSpanId.isRoot => v shouldBe f"${traceIdValue.hex128}-${spanIdValue.hex64}-${if (sampled) "1" else "0"}"
            case B3Header => v shouldBe f"${traceIdValue.hex128}-${spanIdValue.hex64}-${if (sampled) "1" else "0"}-${parentSpanIdValue.hex64}"
          })
        }
      }
    }

    "lists the B3 headers" in {
      B3SingleHeaderFormatter.fields shouldBe Seq(B3Header)
    }

    "copy the request headers to the response" in {
      val setHeader = mock[(String, String) => Unit]

      B3SingleHeaderFormatter.setResponseHeaders({
        case B3Header => B3Header
      }, setHeader)

      verify(setHeader).apply(B3Header, B3Header)
      verifyNoMoreInteractions(setHeader)
    }

    def sampledFlag(sampled: Boolean, debug: Boolean): String = (sampled, debug) match {
      case (_, true) => "d"
      case (true, false) => "1"
      case (false, false) => "0"
    }
  }
}
