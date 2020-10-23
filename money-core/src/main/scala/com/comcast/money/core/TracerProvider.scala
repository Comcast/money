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

package com.comcast.money.core

import com.comcast.money.api.{ InstrumentationLibrary, MoneyTracer, MoneyTracerProvider, SpanFactory, SpanHandler }
import com.comcast.money.core.formatters.Formatter

import scala.collection.concurrent.TrieMap

trait TracerProvider extends MoneyTracerProvider {
  val clock: Clock
  val formatter: Formatter
  val handler: SpanHandler

  private val tracers = new TrieMap[InstrumentationLibrary, Tracer]()

  override def get(instrumentationName: String): MoneyTracer =
    get(new InstrumentationLibrary(instrumentationName, null))

  override def get(instrumentationName: String, instrumentationVersion: String): MoneyTracer =
    get(new InstrumentationLibrary(instrumentationName, instrumentationVersion))

  def get(instrumentationLibrary: InstrumentationLibrary): Tracer = {
    tracers.getOrElseUpdate(instrumentationLibrary, {
      new Tracer {
        override val spanFactory: SpanFactory = new CoreSpanFactory(clock, handler, formatter, instrumentationLibrary)
      }
    })
  }
}
