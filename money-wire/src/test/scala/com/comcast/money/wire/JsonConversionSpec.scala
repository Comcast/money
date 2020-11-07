/*
 * Copyright 2012 Comcast Cable Communications Management, LLC
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

import com.comcast.money.api.{ Note, SpanInfo }
import com.comcast.money.core.formatters.FormatterUtils.randomRemoteSpanId
import io.opentelemetry.api.trace.StatusCode
import org.scalatest.Inspectors
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class JsonConversionSpec extends AnyWordSpec with Matchers with Inspectors {

  import JsonConversions._

  import scala.collection.JavaConverters._

  val orig = TestSpanInfo(
    id = randomRemoteSpanId(),
    name = "key",
    appName = "app",
    host = "host",
    startTimeNanos = 1000000L,
    endTimeNanos = 1035000L,
    status = StatusCode.OK,
    durationNanos = 35000L,
    notes = Map[String, Note[_]](
      "what" -> Note.of("what", 1L),
      "when" -> Note.of("when", 2L),
      "bob" -> Note.of("bob", "craig"),
      "none" -> Note.of("none", null),
      "bool" -> Note.of("bool", true),
      "dbl" -> Note.of("dbl", 1.0)).asJava).asInstanceOf[SpanInfo]

  "Json Conversion" should {
    "roundtrip" in {

      val json = orig.convertTo[String]
      val converted = json.convertTo[SpanInfo]

      converted.appName shouldEqual orig.appName
      converted.name shouldEqual orig.name
      converted.durationMicros shouldEqual orig.durationMicros
      converted.host shouldEqual orig.host
      converted.id shouldEqual orig.id
      converted.success shouldEqual orig.success
      converted.startTimeMillis shouldEqual orig.startTimeMillis
      converted.notes shouldEqual orig.notes
    }
  }
}
