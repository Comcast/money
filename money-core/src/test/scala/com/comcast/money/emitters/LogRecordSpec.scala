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

package com.comcast.money.emitters

import org.scalatest.{BeforeAndAfter, Matchers, OneInstancePerTest, WordSpec}

class LogRecordSpec extends WordSpec with Matchers with OneInstancePerTest with BeforeAndAfter {

  before {
    LogRecord.clear()
  }

  "adding a message to a log" should {
    "result in the message being accessible" in {
      LogRecord.add("log", "foo")
      LogRecord.contains("log")(_.equals("foo"))
    }
    "the set of messages in the log contains the added message" in {
      LogRecord.add("log", "foo")
      LogRecord.log("log") should contain("foo")
    }
  }
  "clearing the log record" should {
    "remove all logs and messages that were recorded" in {
      LogRecord.add("log", "foo")
      LogRecord.add("twig", "bar")
      LogRecord.clear()

      LogRecord.log("log") shouldBe empty
      LogRecord.log("twig") shouldBe empty
    }
  }
}
