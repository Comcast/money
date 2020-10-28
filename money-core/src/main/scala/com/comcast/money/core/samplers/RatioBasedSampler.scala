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

import com.comcast.money.api.{ Note, SpanId }

/**
 * A sampler that calculates that a ratio of spans should be recorded based on the lower 64-bits of the trace id.
 * @param ratio of spans to be recorded and sampled, between 0.0 and 1.0
 */
final class RatioBasedSampler(val ratio: Double) extends Sampler {

  private val upperBound =
    if (ratio <= 0.0) {
      0L
    } else if (ratio >= 1.0) {
      Long.MaxValue
    } else {
      (Long.MaxValue * ratio).toLong
    }

  override def shouldSample(spanId: SpanId, parentSpanId: Option[SpanId], name: String): SamplerResult =
    if (spanId.traceIdAsUUID.getLeastSignificantBits.abs < upperBound) {
      SamplerResult.RecordAndSample.withNote(Note.of("sampling.probability", ratio))
    } else
      SamplerResult.Drop
}
