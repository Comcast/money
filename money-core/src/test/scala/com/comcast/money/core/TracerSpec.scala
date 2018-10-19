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

import com.comcast.money.api.{ Note, SpanId, Span, SpanFactory }
import com.comcast.money.core.handlers.TestData
import com.comcast.money.core.internal.SpanLocal
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ OneInstancePerTest, BeforeAndAfterEach, Matchers, WordSpec }

class TracerSpec extends WordSpec
  with Matchers with MockitoSugar with TestData with BeforeAndAfterEach with OneInstancePerTest {

  val mockSpanFactory = mock[SpanFactory]
  val mockSpan = mock[Span]
  val noteCaptor = ArgumentCaptor.forClass(classOf[Note[_]])
  val underTest = new Tracer {
    val spanFactory = mockSpanFactory
  }

  override def beforeEach() = {
    SpanLocal.clear()

    doReturn(mockSpan).when(mockSpanFactory).newSpan(any[SpanId], anyString())
    doReturn(mockSpan).when(mockSpanFactory).newSpan(anyString())
    doReturn(mockSpan).when(mockSpanFactory).childSpan(anyString(), any[Span])
    doReturn(testSpanInfo).when(mockSpan).info()
  }

  "Tracer" should {
    "start a new span when no span exists" in {
      underTest.startSpan("foo")

      verify(mockSpan).start()

      SpanLocal.current shouldBe Some(mockSpan)
    }

    "start a child span if a span already exsits" in {
      SpanLocal.push(testSpan)

      underTest.startSpan("bar")

      verify(mockSpanFactory).childSpan("bar", testSpan)
      verify(mockSpan).start()

      SpanLocal.current shouldBe Some(mockSpan)
    }

    "record a time" in {
      SpanLocal.push(mockSpan)

      underTest.time("foo")

      verify(mockSpan).record(any[Note[_]])
    }

    "record a double" in {
      SpanLocal.push(mockSpan)

      underTest.record("dbl", 1.2)

      verify(mockSpan).record(noteCaptor.capture())

      val note = noteCaptor.getValue

      note.name shouldBe "dbl"
      note.value shouldBe 1.2
    }

    "record a sticky double" in {
      SpanLocal.push(mockSpan)

      underTest.record("dbl", 1.2, true)

      verify(mockSpan).record(noteCaptor.capture())

      val note = noteCaptor.getValue

      note.name shouldBe "dbl"
      note.value shouldBe 1.2
      note.isSticky shouldBe true
    }

    "record a string" in {
      SpanLocal.push(mockSpan)

      underTest.record("str", "bar")

      verify(mockSpan).record(noteCaptor.capture())

      val note = noteCaptor.getValue

      note.name shouldBe "str"
      note.value shouldBe "bar"
    }

    "record a sticky string" in {
      SpanLocal.push(mockSpan)

      underTest.record("str", "bar", true)

      verify(mockSpan).record(noteCaptor.capture())

      val note = noteCaptor.getValue

      note.name shouldBe "str"
      note.value shouldBe "bar"
      note.isSticky shouldBe true
    }

    "record a long" in {
      SpanLocal.push(mockSpan)

      underTest.record("lng", 100L)

      verify(mockSpan).record(noteCaptor.capture())

      val note = noteCaptor.getValue

      note.name shouldBe "lng"
      note.value shouldBe 100L
    }

    "record a sticky long" in {
      SpanLocal.push(mockSpan)

      underTest.record("lng", 100L, true)

      verify(mockSpan).record(noteCaptor.capture())

      val note = noteCaptor.getValue

      note.name shouldBe "lng"
      note.value shouldBe 100L
      note.isSticky shouldBe true
    }

    "record a boolean" in {
      SpanLocal.push(mockSpan)

      underTest.record("bool", true)

      verify(mockSpan).record(noteCaptor.capture())

      val note = noteCaptor.getValue

      note.name shouldBe "bool"
      note.value shouldBe true
    }

    "record a sticky boolean" in {
      SpanLocal.push(mockSpan)

      underTest.record("bool", true, true)

      verify(mockSpan).record(noteCaptor.capture())

      val note = noteCaptor.getValue

      note.name shouldBe "bool"
      note.value shouldBe true
      note.isSticky shouldBe true
    }

    "record a note" in {
      SpanLocal.push(mockSpan)

      underTest.record(testLongNote)

      verify(mockSpan).record(noteCaptor.capture())

      val note = noteCaptor.getValue

      note.name shouldBe testLongNote.name
      note.value shouldBe testLongNote.value
    }

    "stop the current span" in {
      SpanLocal.push(mockSpan)

      underTest.stopSpan(true)

      verify(mockSpan).stop(true)
    }

    "start a timer" in {
      SpanLocal.push(mockSpan)

      underTest.startTimer("timer")

      verify(mockSpan).startTimer("timer")
    }

    "stop a timer" in {
      SpanLocal.push(mockSpan)

      underTest.stopTimer("t")

      verify(mockSpan).stopTimer("t")
    }

    "stop the span on close" in {
      SpanLocal.push(mockSpan)

      underTest.close()

      verify(mockSpan).stop(true)
    }
  }
}
