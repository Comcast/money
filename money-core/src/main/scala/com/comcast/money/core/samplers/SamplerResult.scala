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

package com.comcast.money.core.samplers

import com.comcast.money.api.Note

/**
 * The result for testing whether or not a new span should be recorded or sampled.
 */
sealed trait SamplerResult

/**
 * Specifies that the span should be dropped and not recorded or sampled.  The span id will be propagated in the current
 * context but the span will not be reported to the [[com.comcast.money.api.SpanHandler]] and no notes will be recorded.
 */
case object DropResult extends SamplerResult

/**
 * Specifies that the span will be recorded along with all notes.
 * @param sample indicates whether the span will be marked as sampled which will be propagated to upstream systems
 * @param notes to be recorded on the span
 */
final case class RecordResult(sample: Boolean = true, notes: Seq[Note[_]] = Nil) extends SamplerResult {

  /**
   * Adds a note to the sampler result to be recorded on the created span
   */
  def withNote(note: Note[_]): SamplerResult = withNotes(Seq(note))

  /**
   * Adds notes to the sampler result to be recorded on the created span
   */
  def withNotes(notes: Seq[Note[_]]): SamplerResult = RecordResult(sample, this.notes ++ notes)
}

object SamplerResult {
  val Drop: SamplerResult = DropResult
  val Record: RecordResult = RecordResult(sample = false)
  val RecordAndSample: RecordResult = RecordResult()
}