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

package com.comcast.money.core.internal

import com.comcast.money.api.SpanId
import com.comcast.money.core.handlers.TestData
import org.slf4j.MDC
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ OneInstancePerTest, BeforeAndAfterEach }
import org.scalatestplus.mockito.MockitoSugar

class SpanLocalSpec extends AnyWordSpec
  with Matchers with OneInstancePerTest with BeforeAndAfterEach with MockitoSugar with TestData {

  override def afterEach(): Unit = {
    SpanLocal.clear()
  }

  "SpanLocal" when {
    "an item exists in span local" should {
      "return the span local value" in {
        SpanLocal.push(testSpan)
        SpanLocal.current shouldEqual Some(testSpan)
      }
      "return None with no span local value" in {
        SpanLocal.current shouldEqual None
      }
      "clear the stored value" in {
        SpanLocal.push(testSpan)

        SpanLocal.clear()
        SpanLocal.current shouldEqual None
      }
      "do nothing if trying to push a null value" in {
        SpanLocal.push(testSpan)
        SpanLocal.push(null)
        SpanLocal.current shouldEqual Some(testSpan)
      }
      "add to the existing call stack" in {
        val nested = testSpan.copy(id = SpanId.createNew())

        SpanLocal.push(testSpan)
        SpanLocal.push(nested)
        SpanLocal.current shouldEqual Some(nested)
      }
      "close the last added item from the call stack" in {
        val nested = testSpan.copy(id = SpanId.createNew())
        SpanLocal.push(testSpan)
        val scope = SpanLocal.push(nested)

        SpanLocal.current shouldEqual Some(nested)

        scope.close()
        SpanLocal.current shouldEqual Some(testSpan)
      }
    }
  }
}
