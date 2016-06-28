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

package com.comcast.money.core.akka

import com.comcast.money.api.{ Span, SpanId }
import com.comcast.money.core.CoreSpan
import com.comcast.money.core.handlers.HandlerChain
import org.scalatest._
import org.scalatest.mock.MockitoSugar

class MoneyActorSpec extends WordSpecLike with Matchers with BeforeAndAfterEach with MockitoSugar {

  override def beforeEach() {
    StackedSpanContext.Implicits.root.clear
  }

  "SpanContext" must {
    "allow simple stacking" in {

      val rootSpan = mock[Span]
      val childSpan = mock[Span]

      val underTest = new RootSpanContext

      underTest.current should be(None)
      underTest.pop() should be(None)

      underTest.push(rootSpan)

      underTest.pop().get should be(rootSpan)
      underTest.pop() should be(None)

      underTest.push(rootSpan)

      val child = new BaseSpanContext()(underTest)
      child.push(childSpan)

      child.pop().get should be(childSpan)
      child.pop().get should be(rootSpan)
      child.pop() should be(None)

    }
    "support a reasonable toString method" in {
      val underTest = new RootSpanContext()
      val child = new BaseSpanContext()(underTest)
      child.push(new CoreSpan(new SpanId("traceId", 23, 42), "StringTest", new HandlerChain(List())))

      underTest.toString should be("RootSpanContext(Stack(), None)")
      child.toString should be("BaseSpanContext(Stack(CoreSpan(SpanId~traceId~23~42,StringTest,HandlerChain(List()))), Some(RootSpanContext(Stack(), None)))")
    }

    "support empty iterations" in {
      val underTest = new RootSpanContext()

      underTest.count(span => true) should be(1)
      underTest.foldLeft(0)(_ + _.spanId.size) should be(0)
    }

    "support simple iterations" in {
      val underTest = new RootSpanContext()
      val child = new BaseSpanContext()(underTest)
      underTest.push(mock[Span])

      child.push(mock[Span])
      child.push(mock[Span])

      // There are two SpanContexts
      // There are two SpanContexts
      child.count(span => true) should be(2)
      // should behave the same:
      child.seq.size should be(2)

      // And three Spans in total
      child.foldLeft(0)(_ + _.spanId.size) should be(3)

      child.clear()
      child.foldLeft(0)(_ + _.spanId.size) should be(0)
    }

    "support a global SpanContext" in {
      StackedSpanContext.Implicits.root.push(mock[Span])
      StackedSpanContext.root.push(mock[Span])

      // It is the same stack - thus they should add up
      StackedSpanContext.Implicits.root.spanId.size should be(2)
    }
  }
}
