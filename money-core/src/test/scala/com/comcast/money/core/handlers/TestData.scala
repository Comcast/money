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

package com.comcast.money.core.handlers

import java.util.Collections

import com.comcast.money.api.{ InstrumentationLibrary, Note, SpanHandler, SpanId, SpanInfo }
import com.comcast.money.core.{ Clock, CoreSpan, CoreSpanInfo, SystemClock }
import com.typesafe.config.Config
import io.opentelemetry.trace.{ StatusCanonicalCode, TraceFlags, TraceState, Span => OtelSpan }

class ConfiguredHandler extends ConfigurableHandler {

  var calledConfigure = false

  def configure(config: Config): Unit = calledConfigure = true
  def handle(span: SpanInfo): Unit = ()
}

class NonConfiguredHandler extends SpanHandler {
  def handle(span: SpanInfo): Unit = ()
}

trait TestData {

  import scala.collection.JavaConverters._

  val testStringNote = Note.of("str", "bar")
  val testLongNote = Note.of("lng", 200L)
  val testDoubleNote = Note.of("dbl", 1.2)
  val testBooleanNote = Note.of("bool", true)

  val clock: Clock = SystemClock

  val testSpanInfo = CoreSpanInfo(
    id = SpanId.createNew(),
    startTimeNanos = clock.now,
    endTimeNanos = clock.now,
    durationNanos = 123456000L,
    status = StatusCanonicalCode.OK,
    name = "test-span",
    appName = "test",
    host = "localhost",
    notes = Map[String, Note[_]]("str" -> testStringNote, "lng" -> testLongNote, "dbl" -> testDoubleNote, "bool" -> testBooleanNote).asJava,
    events = Collections.emptyList())

  val testSpanId = SpanId.createNew()
  val testSpan = CoreSpan(testSpanId, "test-span", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)
  val childSpanId = testSpanId.createChild()
  val childSpan = CoreSpan(childSpanId, "child-span", OtelSpan.Kind.INTERNAL, InstrumentationLibrary.UNKNOWN, SystemClock, null)

  val fixedTestSpanId = SpanId.createRemote("5092ddfe-3701-4f84-b3d2-21f5501c0d28", 5176425846116696835L, 5176425846116696835L, TraceFlags.getSampled, TraceState.getDefault)
  val fixedTestSpanInfo = CoreSpanInfo(
    id = fixedTestSpanId,
    startTimeNanos = 100000000L,
    endTimeNanos = 300000000L,
    durationNanos = 200000L,
    status = StatusCanonicalCode.OK,
    name = "test-span",
    appName = "test",
    host = "localhost",
    notes = Map[String, Note[_]]("str" -> testStringNote, "lng" -> testLongNote, "dbl" -> testDoubleNote, "bool" -> testBooleanNote).asJava,
    events = Collections.emptyList())
}
