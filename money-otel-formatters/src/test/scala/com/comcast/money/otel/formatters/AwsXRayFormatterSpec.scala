package com.comcast.money.otel.formatters

import com.comcast.money.api.SpanId
import com.comcast.money.core.TraceGenerators
import com.comcast.money.otel.formatters.AwsXRayFormatter.AmznTraceIdHeader
import org.mockito.Mockito.{verify, verifyNoMoreInteractions}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.collection.mutable

class AwsXRayFormatterSpec extends AnyWordSpec with MockitoSugar with Matchers with ScalaCheckDrivenPropertyChecks with TraceGenerators {
  val underTest = new AwsXRayFormatter()
  val nullString = null.asInstanceOf[String]

  "AwsXRayFormatter" should {
    "can roundtrip a span id" in {
      val spanId = SpanId.createNew()

      val map = mutable.Map[String, String]()

      underTest.toHttpHeaders(spanId, map.put)

      val result = underTest.fromHttpHeaders(k => map.getOrElse(k, nullString))

      result shouldBe Some(spanId)
    }

    "lists the headers" in {
      underTest.fields shouldBe Seq(AmznTraceIdHeader)
    }

    "copy the request headers to the response" in {
      val setHeader = mock[(String, String) => Unit]

      underTest.setResponseHeaders({
        case AmznTraceIdHeader => AmznTraceIdHeader
      }, setHeader)

      verify(setHeader).apply(AmznTraceIdHeader, AmznTraceIdHeader)
      verifyNoMoreInteractions(setHeader)
    }
  }
}
