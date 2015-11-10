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

package com.comcast.money.wire

import com.comcast.money.core.{ LongNote, Note, SpanId, Span }
import org.scalatest.{ Inspectors, Matchers, WordSpec }

class JsonConversionSpec extends WordSpec with Matchers with Inspectors {

  import JsonConversions._

  val orig = Span(
    SpanId(1L), "key", "app", "host", 1L, true, 35L,
    Map(
      "what" -> Note("what", 1L, 100L),
      "when" -> Note("when", 2L, 200L),
      "bob" -> Note("bob", "craig", 300L),
      "none" -> LongNote("none", None),
      "bool" -> Note("bool", true, 400L),
      "dbl" -> Note("dbl", 1.0, 500L)
    )
  )

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
