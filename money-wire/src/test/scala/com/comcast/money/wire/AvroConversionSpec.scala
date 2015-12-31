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

import com.comcast.money.api.{ Note, SpanId }
import com.comcast.money.core.Span
import org.scalatest.{ Inspectors, Matchers, WordSpec }

class AvroConversionSpec extends WordSpec with Matchers with Inspectors {

  import AvroConversions._

  "Avro Conversion" should {
    "roundtrip" in {
      val orig = Span(
        new SpanId("foo", 1L), "key", "app", "host", 1L, true, 35L,
        Map(
          "what" -> Note.of("what", 1L),
          "when" -> Note.of("when", 2L),
          "bob" -> Note.of("bob", "craig"),
          "none" -> Note.of("none", null),
          "bool" -> Note.of("bool", true),
          "dbl" -> Note.of("dbl", 1.0)
        )
      )

      val bytes = orig.convertTo[Array[Byte]]
      val roundtrip = bytes.convertTo[Span]

      roundtrip.appName shouldEqual orig.appName
      roundtrip.spanName shouldEqual orig.spanName
      roundtrip.duration shouldEqual orig.duration
      roundtrip.host shouldEqual orig.host
      roundtrip.spanId shouldEqual orig.spanId
      roundtrip.success shouldEqual orig.success
      roundtrip.startTime shouldEqual orig.startTime
      roundtrip.notes shouldEqual orig.notes
    }
  }
}
