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

import com.comcast.money.api.{ InstrumentationLibrary, SpanFactory }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MoneyTraceProviderSpec extends AnyWordSpec with Matchers {

  "MoneyTraceProvider" should {
    "wrap an existing Tracer with a decorated SpanFactory" in {
      val factory = new CoreSpanFactory(SystemClock, DisabledSpanHandler, DisabledFormatter, Money.InstrumentationLibrary)
      val tracer = new Tracer {
        override val spanFactory: SpanFactory = factory
      }

      val underTest = MoneyTracerProvider(tracer)

      val result = underTest.get("test")
      result shouldBe a[Tracer]
      result should not be tracer

      val libraryTracer = result.asInstanceOf[Tracer]
      val span = libraryTracer.startSpan("test")
      val info = span.info
      info.library shouldBe new InstrumentationLibrary("test")
    }

    "returns the same Tracer for equivalent InstrumentationLibraries" in {
      val factory = new CoreSpanFactory(SystemClock, DisabledSpanHandler, DisabledFormatter, Money.InstrumentationLibrary)
      val tracer = new Tracer {
        override val spanFactory: SpanFactory = factory
      }

      val underTest = MoneyTracerProvider(tracer)

      val result = underTest.get("test", "0.0.1")
      val other = underTest.get("test", "0.0.1")
      other shouldBe result
    }

    "return different Tracers for different InstrumentationLibraries" in {
      val factory = new CoreSpanFactory(SystemClock, DisabledSpanHandler, DisabledFormatter, Money.InstrumentationLibrary)
      val tracer = new Tracer {
        override val spanFactory: SpanFactory = factory
      }

      val underTest = MoneyTracerProvider(tracer)

      val result = underTest.get("test", "0.0.1")
      val other = underTest.get("test", "0.0.2")
      other shouldBe a[Tracer]
      other should not be result
    }
  }
}
