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

import com.comcast.money.api.{ Note, SpanId, SpanInfo }
import com.comcast.money.core.CoreSpanInfo
import org.scalatest.{ Inspectors, Matchers, WordSpec }

class JsonConversionSpec extends WordSpec with Matchers with Inspectors {

  import JsonConversions._

  import scala.collection.JavaConversions._

  val orig = CoreSpanInfo(
    id = new SpanId("foo", 1L),
    name = "key",
    appName = "app",
    host = "host",
    startTimeMillis = 1L,
    success = true,
    durationMicros = 35L,
    notes = Map(
      "what" -> Note.of("what", 1L),
      "when" -> Note.of("when", 2L),
      "bob" -> Note.of("bob", "craig"),
      "none" -> Note.of("none", null),
      "bool" -> Note.of("bool", true),
      "dbl" -> Note.of("dbl", 1.0))).asInstanceOf[SpanInfo]

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
