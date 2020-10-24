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

import java.util
import java.util.Collections

import com.comcast.money.api.{ Note, Sampler, SpanId }

class PercentageBasedSampler(percentage: Double) extends Sampler {

  private val upperBound =
    if (percentage <= 0.0) {
      0L
    } else if (percentage >= 1.0) {
      Long.MaxValue
    } else {
      (Long.MaxValue * percentage).toLong
    }

  override def shouldSample(spanId: SpanId, parentSpanId: Option[SpanId], name: String): Sampler.Result =
    if (spanId.traceIdAsUUID.getLeastSignificantBits.abs < upperBound) new Sampler.Result {
      override def decision(): Sampler.Decision = Sampler.Decision.SAMPLE_AND_RECORD
      override def notes(): util.Collection[Note[_]] = Collections.singletonList(Note.of("sampling.probability", percentage))
    }
    else Drop
}
