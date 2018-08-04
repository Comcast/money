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

package com.comcast.money.core

import com.comcast.money.api.{ SpanInfo, SpanHandler, Note, SpanId }
import com.comcast.money.core.handlers.TestData
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }

class CoreSpanSpec extends WordSpec with Matchers with TestData with MockitoSugar {

  "CoreSpan" should {
    "set the startTimeMillis and startTimeMicros when started" in {
      val underTest = CoreSpan(new SpanId(), "test", null)
      underTest.start()

      val state = underTest.info

      state.startTimeMicros.toInt should not be 0
      state.startTimeMillis.toInt should not be 0
    }

    "record a timer" in {
      val underTest = CoreSpan(new SpanId(), "test", null)

      underTest.startTimer("foo")
      underTest.stopTimer("foo")

      underTest.info.notes should contain key "foo"
    }

    "record a note" in {
      val underTest = CoreSpan(new SpanId(), "test", null)

      underTest.record(testLongNote)

      underTest.info.notes should contain value testLongNote
    }

    "set the endTimeMillis and endTimeMicros when stopped" in {
      val handler = mock[SpanHandler]
      val underTest = CoreSpan(new SpanId(), "test", handler)

      underTest.stop(true)

      val state = underTest.info

      state.endTimeMicros.toInt should not be 0
      state.endTimeMillis.toInt should not be 0
    }

    "invoke the span handler when stopped" in {
      val handler = mock[SpanHandler]
      val handleCaptor = ArgumentCaptor.forClass(classOf[SpanInfo])
      val underTest = CoreSpan(new SpanId(), "test", handler)

      underTest.start()
      underTest.record(testLongNote)
      underTest.stop(true)

      verify(handler).handle(handleCaptor.capture())

      val handledInfo = handleCaptor.getValue

      handledInfo.id shouldBe underTest.id
      handledInfo.startTimeMicros.toInt should not be 0
      handledInfo.startTimeMillis.toInt should not be 0
      handledInfo.endTimeMicros.toInt should not be 0
      handledInfo.endTimeMillis.toInt should not be 0
      handledInfo.notes should contain value testLongNote
    }
  }
}
