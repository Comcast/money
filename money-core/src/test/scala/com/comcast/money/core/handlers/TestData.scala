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

package com.comcast.money.core.handlers

import com.comcast.money.api.{ SpanHandler, SpanInfo, Note, SpanId }
import com.comcast.money.core.{ CoreSpan, CoreSpanInfo }
import com.typesafe.config.Config

class ConfiguredHandler extends ConfigurableHandler {

  var calledConfigure = false

  def configure(config: Config): Unit = calledConfigure = true
  def handle(span: SpanInfo): Unit = ()
}

class NonConfiguredHandler extends SpanHandler {
  def handle(span: SpanInfo): Unit = ()
}

trait TestData {

  import scala.collection.JavaConversions._

  val testStringNote = Note.of("str", "bar")
  val testLongNote = Note.of("lng", 200L)
  val testDoubleNote = Note.of("dbl", 1.2)
  val testBooleanNote = Note.of("bool", true)

  val testSpanInfo = CoreSpanInfo(
    id = new SpanId(),
    startTimeMillis = System.currentTimeMillis,
    startTimeMicros = System.nanoTime() / 1000,
    endTimeMillis = System.currentTimeMillis,
    endTimeMicros = System.nanoTime / 1000,
    durationMicros = 123456L,
    name = "test-span",
    appName = "test",
    host = "localhost",
    notes = Map("str" -> testStringNote, "lng" -> testLongNote, "dbl" -> testDoubleNote, "bool" -> testBooleanNote)
  )

  val testSpan = CoreSpan(new SpanId(), "test-span", null)

  val fixedTestSpanInfo = CoreSpanInfo(
    id = new SpanId("5092ddfe-3701-4f84-b3d2-21f5501c0d28", 5176425846116696835L, 5176425846116696835L),
    startTimeMillis = 100L,
    startTimeMicros = 100000L,
    endTimeMillis = 300L,
    endTimeMicros = 300000L,
    durationMicros = 200L,
    name = "test-span",
    appName = "test",
    host = "localhost",
    notes = Map("str" -> testStringNote, "lng" -> testLongNote, "dbl" -> testDoubleNote, "bool" -> testBooleanNote)
  )
}
