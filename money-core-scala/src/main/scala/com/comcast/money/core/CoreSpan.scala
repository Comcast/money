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

import java.lang.Long

import com.comcast.money.api._

import scala.collection.JavaConversions._
import scala.collection.concurrent.TrieMap

/**
 * A mutable implementation of the Span that also includes SpanInfo
 *
 * @param id The [[SpanId]]
 * @param name The name of the span
 * @param handler The [[SpanHandler]] responsible for processing the span once it is stopped
 */
case class CoreSpan(
    id: SpanId,
    name: String,
    handler: SpanHandler
) extends Span {

  private var startTimeMillis: Long = 0L
  private var startTimeMicros: Long = 0L
  private var endTimeMillis: Long = 0L
  private var endTimeMicros: Long = 0L
  private var success: java.lang.Boolean = true

  // use concurrent maps
  private val timers = new TrieMap[String, Long]()
  private val noted = new TrieMap[String, Note[_]]()

  def start(): Unit = {
    startTimeMillis = System.currentTimeMillis
    startTimeMicros = System.nanoTime / 1000
  }

  def stop(): Unit = stop(true)

  def stop(result: java.lang.Boolean): Unit = {
    endTimeMillis = System.currentTimeMillis
    endTimeMicros = System.nanoTime / 1000
    this.success = result

    // process any hanging timers
    val openTimers = timers.keys
    openTimers.foreach(stopTimer)

    handler.handle(info)
  }

  def stopTimer(timerKey: String): Unit =
    timers.remove(timerKey) foreach {
      timerStartInstant =>
        record(Note.of(timerKey, System.nanoTime - timerStartInstant))
    }

  def record(note: Note[_]): Unit = noted += note.name -> note

  def startTimer(timerKey: String): Unit = timers += timerKey -> System.nanoTime

  def info(): SpanInfo =
    CoreSpanInfo(
      id,
      name,
      startTimeMillis,
      startTimeMicros,
      endTimeMillis,
      endTimeMicros,
      calculateDuration,
      success,
      noted.toMap[String, Note[_]]
    )

  private def calculateDuration: Long =
    if (endTimeMicros <= 0L && startTimeMicros <= 0L)
      0L
    else if (endTimeMicros <= 0L)
      (System.nanoTime() / 1000) - startTimeMicros
    else
      endTimeMicros - startTimeMicros
}
