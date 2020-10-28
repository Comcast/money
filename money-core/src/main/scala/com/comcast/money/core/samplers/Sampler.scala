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

import com.comcast.money.api.SpanId

/**
 * A sampler that tests whether or not a span should be recorded or sampled.
 */
trait Sampler {
  /**
   * Tests the span to determine if it should be recorded or sampled.
   * @param spanId the id of the span
   * @param parentSpanId the id of the parent span, if any
   * @param spanName the name of the span
   * @return a [[SamplerResult]] indicating whether or not the span should be recorded or sampled
   */
  def shouldSample(spanId: SpanId, parentSpanId: Option[SpanId], spanName: String): SamplerResult
}
