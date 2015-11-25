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

import com.typesafe.config.Config

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class LogRecorder extends SpanEmitter {
  import LogRecorder._

  override def configure(emitterConf: Config): Unit = {
    println("\r\n\r\n!!!configure log recorder!!")
  }

  override def emit(spanData: SpanData): Unit = {
    println("\r\n!!!received span " + spanData.toString)
    messages.append(spanData)
  }
}

object LogRecorder {

  val messages = new mutable.ArrayBuffer[SpanData]()

  def reset(): Unit = messages.clear()

  def expectSpan(within: Duration, f: SpanData => Boolean) =
    Await.result[Boolean](Future {
        while(!messages.exists(f)) {
          Thread.sleep(10)
        }
        true
      }, within)
}
