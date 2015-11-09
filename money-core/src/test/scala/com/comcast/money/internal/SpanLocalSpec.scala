package com.comcast.money.internal

import com.comcast.money.core.SpanId
import org.scalatest.mock.MockitoSugar

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.slf4j.MDC

class SpanLocalSpec extends WordSpec with Matchers with BeforeAndAfterEach with MockitoSugar {

  override def afterEach() = {
    SpanLocal.clear()
  }

  "SpanLocal" when {
    "an item exists in span local" should {
      "return the span local value" in {
        val spanId = SpanId("1", 2L, 3L)
        SpanLocal.push(spanId)
        SpanLocal.current shouldEqual Some(spanId)
      }
      "clear the stored value" in {
        val spanId = SpanId("1", 2L, 3L)
        SpanLocal.push(spanId)

        SpanLocal.clear()
        SpanLocal.current shouldEqual None
      }
      "do nothing if trying to push a null value" in {
        val spanId = SpanId("1", 2L, 3L)
        SpanLocal.push(spanId)
        SpanLocal.current shouldEqual Some(spanId)

        SpanLocal.push(null)
        SpanLocal.current shouldEqual Some(spanId)
      }
      "add to the existing call stack" in {
        val spanId = SpanId("1", 2L, 3L)
        SpanLocal.push(spanId)
        SpanLocal.current shouldEqual Some(spanId)

        val nestedSpanId = SpanId("2", 3L, 4L)
        SpanLocal.push(nestedSpanId)
        SpanLocal.current shouldEqual Some(nestedSpanId)
      }
      "pop the last added item from the call stack" in {
        val spanId = SpanId("1", 2L, 3L)
        SpanLocal.push(spanId)
        SpanLocal.current shouldEqual Some(spanId)

        val nestedSpanId = SpanId("2", 3L, 4L)
        SpanLocal.push(nestedSpanId)
        SpanLocal.current shouldEqual Some(nestedSpanId)

        val popped = SpanLocal.pop()
        popped shouldEqual Some(nestedSpanId)
        SpanLocal.current shouldEqual Some(spanId)
      }
      "set the MDC value on push" in {
        val spanId = SpanId()
        SpanLocal.push(spanId)

        MDC.get("moneyTrace") shouldEqual MDCSupport.format(spanId)
      }
      "remove the MDC value on pop" in {
        val spanId = SpanId()
        SpanLocal.push(spanId)

        MDC.get("moneyTrace") shouldEqual MDCSupport.format(spanId)
        SpanLocal.pop()

        MDC.get("moneyTrace") shouldBe null
      }
      "remove the MDC value on clear" in {
        val spanId = SpanId()
        SpanLocal.push(spanId)

        MDC.get("moneyTrace") shouldEqual MDCSupport.format(spanId)
        SpanLocal.clear()

        MDC.get("moneyTrace") shouldBe null
      }
    }
  }
}
