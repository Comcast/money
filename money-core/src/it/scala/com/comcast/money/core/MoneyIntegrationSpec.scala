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

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.concurrent.duration._

trait MoneyIntegrationSpec extends WordSpecLike with Matchers with BeforeAndAfterEach {
  import scala.collection.JavaConversions._

  override def beforeEach() = {
    LogRecorder.reset()
  }

  def expectSpan(f: SpanData => Boolean, within: Duration = 500 milliseconds) =
    LogRecorder.expectSpan(within, f)

  def expectSpanNamed(name: String, within: Duration = 500 milliseconds) =
    expectSpan(_.getName == name, within)

  def expectSpanResult(result: Boolean, within: Duration = 500 milliseconds) =
    expectSpan(_.isSuccess == result, within)

  def expectSpanWithNote(f: Note[_] => Boolean, within: Duration = 500 milliseconds) =
    expectSpan(_.getNotes.values.exists(f), within)
}
