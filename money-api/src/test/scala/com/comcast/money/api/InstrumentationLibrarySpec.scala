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

package com.comcast.money.api

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class InstrumentationLibrarySpec extends AnyWordSpec with Matchers {

  "An InstrumentationLibrary" should {
    "have the specified name" in {
      val library = new InstrumentationLibrary("test")
      library.name shouldBe "test"
    }

    "have the specified name and version" in {
      val library = new InstrumentationLibrary("test", "0.0.1")
      library.name shouldBe "test"
      library.version shouldBe "0.0.1"
    }

    "support a null version" in {
      val library = new InstrumentationLibrary("test", null)
      library.name shouldBe "test"
      library.version shouldBe null
    }

    "not allow a null name" in {
      assertThrows[NullPointerException] {
        new InstrumentationLibrary(null)
      }
    }

    "support equality" in {
      val lib1 = new InstrumentationLibrary("test", "0.0.1")
      val lib2 = new InstrumentationLibrary("test", "0.0.1")

      lib1 shouldBe lib2
      lib1.hashCode() shouldBe lib2.hashCode()
    }

    "not consider different names to be equal" in {
      val lib1 = new InstrumentationLibrary("foo", "0.0.1")
      val lib2 = new InstrumentationLibrary("bar", "0.0.1")

      lib1 should not be lib2
    }

    "not consider different versions to be equal" in {
      val lib1 = new InstrumentationLibrary("test", "0.0.1")
      val lib2 = new InstrumentationLibrary("test", "0.0.2")

      lib1 should not be lib2
    }

    "format string with only name" in {
      val library = new InstrumentationLibrary("test")
      library.toString shouldBe "test"
    }

    "format string with name and version" in {
      val library = new InstrumentationLibrary("test", "0.0.1")
      library.toString shouldBe "test:0.0.1"
    }
  }
}
