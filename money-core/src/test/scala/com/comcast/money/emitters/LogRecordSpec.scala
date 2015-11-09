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