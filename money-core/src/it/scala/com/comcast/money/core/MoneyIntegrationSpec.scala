package com.comcast.money.core

import com.typesafe.config.Config
import org.scalatest.{Matchers, WordSpec}

import scala.collection._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration

class MoneyIntegrationSpec extends WordSpec with Matchers {

  class LogRecorder extends SpanEmitter {
    val messages = new mutable.ArrayBuffer[SpanData]()

    override def configure(emitterConf: Config): Unit = {}

    override def emit(spanData: SpanData): Unit = {
      messages.append(spanData)
    }

    def reset(): Unit = messages.clear()

    def expectSpan(within: Duration, spanCheck: PartialFunction[SpanData, Boolean]) =
      Await.result[Boolean](Future {
        while(!messages.exists(spanCheck)) {
          Thread.sleep(10)
        }
        true
      }, within)
  }
}
