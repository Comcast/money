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

import com.comcast.money.api.{ IdGenerator, SpanId }
import com.typesafe.config.ConfigFactory
import io.opentelemetry.api.trace.{ TraceFlags, TraceState }
import org.mockito.ArgumentMatchers.{ any, anyString }
import org.mockito.Mockito.{ never, verify, verifyNoMoreInteractions, when }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class ParentBasedSamplerSpec extends AnyWordSpec with MockitoSugar with Matchers {
  "ParentBasedSampler" should {
    "have sensible default samplers" in {
      val underTest = ParentBasedSampler(ConfigFactory.empty)

      underTest.root shouldBe AlwaysOnSampler
      underTest.localSampled shouldBe AlwaysOnSampler
      underTest.localNotSampled shouldBe AlwaysOffSampler
      underTest.remoteSampled shouldBe AlwaysOnSampler
      underTest.remoteNotSampled shouldBe AlwaysOffSampler
    }

    "sets child samplers from configuration" in {
      val config = ConfigFactory.parseString(
        """
          | root = { type = "always-off" }
          | local-sampled = { type = "always-off" }
          | local-not-sampled = { type = "always-on" }
          | remote-sampled = { type = "always-off" }
          | remote-not-sampled = { type = "always-on" }
          |""".stripMargin)

      val underTest = ParentBasedSampler(config)

      underTest.root shouldBe AlwaysOffSampler
      underTest.localSampled shouldBe AlwaysOffSampler
      underTest.localNotSampled shouldBe AlwaysOnSampler
      underTest.remoteSampled shouldBe AlwaysOffSampler
      underTest.remoteNotSampled shouldBe AlwaysOnSampler
    }

    "uses root sampler for root spans" in {

      val root = mock[Sampler]
      val remoteNotSampled = mock[Sampler]
      val remoteSampled = mock[Sampler]
      val localNotSampled = mock[Sampler]
      val localSampled = mock[Sampler]
      val underTest = new ParentBasedSampler(root, remoteSampled, remoteNotSampled, localSampled, localNotSampled)

      val spanId = SpanId.createNew()
      val expected = RecordResult()

      when(underTest.root.shouldSample(spanId, None, "name")).thenReturn(expected)

      val result = underTest.shouldSample(spanId, None, "name")

      result shouldBe expected
      verify(underTest.root).shouldSample(spanId, None, "name")
      verifyNoMoreInteractions(underTest.root, underTest.remoteNotSampled, underTest.remoteSampled, underTest.localNotSampled, underTest.localSampled)
    }

    "uses local sampled sampler for child spans" in {

      val root = mock[Sampler]
      val remoteNotSampled = mock[Sampler]
      val remoteSampled = mock[Sampler]
      val localNotSampled = mock[Sampler]
      val localSampled = mock[Sampler]
      val underTest = new ParentBasedSampler(root, remoteSampled, remoteNotSampled, localSampled, localNotSampled)

      val parentSpanId = SpanId.createNew()
      val childSpanId = parentSpanId.createChild()
      val expected = RecordResult()

      when(underTest.localSampled.shouldSample(childSpanId, Some(parentSpanId), "name")).thenReturn(expected)

      val result = underTest.shouldSample(childSpanId, Some(parentSpanId), "name")

      result shouldBe expected
      verify(underTest.localSampled).shouldSample(childSpanId, Some(parentSpanId), "name")
      verifyNoMoreInteractions(underTest.root, underTest.remoteNotSampled, underTest.remoteSampled, underTest.localNotSampled, underTest.localSampled)
    }

    "uses local not sampled sampler for child spans" in {

      val root = mock[Sampler]
      val remoteNotSampled = mock[Sampler]
      val remoteSampled = mock[Sampler]
      val localNotSampled = mock[Sampler]
      val localSampled = mock[Sampler]
      val underTest = new ParentBasedSampler(root, remoteSampled, remoteNotSampled, localSampled, localNotSampled)

      val parentSpanId = SpanId.createNew(false)
      val childSpanId = parentSpanId.createChild()
      val expected = RecordResult()

      when(underTest.localNotSampled.shouldSample(childSpanId, Some(parentSpanId), "name")).thenReturn(expected)

      val result = underTest.shouldSample(childSpanId, Some(parentSpanId), "name")

      result shouldBe expected
      verify(underTest.localNotSampled).shouldSample(childSpanId, Some(parentSpanId), "name")
      verifyNoMoreInteractions(underTest.root, underTest.remoteNotSampled, underTest.remoteSampled, underTest.localNotSampled, underTest.localSampled)
    }

    "uses remote sampled sampler for child spans" in {

      val root = mock[Sampler]
      val remoteNotSampled = mock[Sampler]
      val remoteSampled = mock[Sampler]
      val localNotSampled = mock[Sampler]
      val localSampled = mock[Sampler]
      val underTest = new ParentBasedSampler(root, remoteSampled, remoteNotSampled, localSampled, localNotSampled)

      val selfId = IdGenerator.generateRandomId()
      val parentSpanId = SpanId.createRemote(IdGenerator.generateRandomTraceId(), selfId, selfId, TraceFlags.getSampled, TraceState.getDefault)
      val childSpanId = parentSpanId.createChild()
      val expected = RecordResult()

      when(underTest.remoteSampled.shouldSample(childSpanId, Some(parentSpanId), "name")).thenReturn(expected)

      val result = underTest.shouldSample(childSpanId, Some(parentSpanId), "name")

      result shouldBe expected
      verify(underTest.remoteSampled).shouldSample(childSpanId, Some(parentSpanId), "name")
      verifyNoMoreInteractions(underTest.root, underTest.remoteNotSampled, underTest.remoteSampled, underTest.localNotSampled, underTest.localSampled)
    }

    "uses remote not sampled sampler for child spans" in {

      val root = mock[Sampler]
      val remoteNotSampled = mock[Sampler]
      val remoteSampled = mock[Sampler]
      val localNotSampled = mock[Sampler]
      val localSampled = mock[Sampler]
      val underTest = new ParentBasedSampler(root, remoteSampled, remoteNotSampled, localSampled, localNotSampled)

      val selfId = IdGenerator.generateRandomId()
      val parentSpanId = SpanId.createRemote(IdGenerator.generateRandomTraceId(), selfId, selfId, TraceFlags.getDefault, TraceState.getDefault)
      val childSpanId = parentSpanId.createChild()
      val expected = RecordResult()

      when(underTest.remoteNotSampled.shouldSample(childSpanId, Some(parentSpanId), "name")).thenReturn(expected)

      val result = underTest.shouldSample(childSpanId, Some(parentSpanId), "name")

      result shouldBe expected
      verify(underTest.remoteNotSampled).shouldSample(childSpanId, Some(parentSpanId), "name")
      verifyNoMoreInteractions(underTest.root, underTest.remoteNotSampled, underTest.remoteSampled, underTest.localNotSampled, underTest.localSampled)
    }
  }
}
