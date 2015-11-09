package com.comcast.money.wire

import com.comcast.money.core.{LongNote, Note, SpanId, Span}
import org.scalatest.{Inspectors, Matchers, WordSpec}

class JsonConversionSpec extends WordSpec with Matchers with Inspectors {

  import JsonConversions._

  val orig = Span(SpanId(1L), "key", "app", "host", 1L, true, 35L,
    Map("what" -> Note("what", 1L, 100L),
      "when" -> Note("when", 2L, 200L),
      "bob" -> Note("bob", "craig", 300L),
      "none" -> LongNote("none", None),
      "bool" -> Note("bool", true, 400L),
      "dbl" -> Note("dbl", 1.0, 500L)
    ))

  "Json Conversion" should {
    "roundtrip" in {

      val json = orig.convertTo[String]
      val converted = json.convertTo[Span]

      converted.appName shouldEqual orig.appName
      converted.spanName shouldEqual orig.spanName
      converted.duration shouldEqual orig.duration
      converted.host shouldEqual orig.host
      converted.spanId shouldEqual orig.spanId
      converted.success shouldEqual orig.success
      converted.startTime shouldEqual orig.startTime
      converted.notes shouldEqual orig.notes
    }
  }
}
