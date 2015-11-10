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

package com.comcast.money.japi

import com.comcast.money.japi.JMoney.TraceSpan
import com.comcast.money.test.AkkaTestJawn
import org.scalatest.{Matchers, OneInstancePerTest, WordSpecLike}

class JMoneySpec extends AkkaTestJawn with WordSpecLike with OneInstancePerTest with Matchers {

  "Java API" should {
    "log a span" in {
      JMoney.startSpan("java-span")
      JMoney.stopSpan(true)
      expectLogMessageContaining("java-span")
    }
    "support closeable" in {
      val span: TraceSpan = JMoney.newSpan("closeable")
      span.close()
      expectLogMessageContaining("closeable")
    }
    "record long values" in {
      JMoney.startSpan("long-values")
      JMoney.record("the-long", java.lang.Long.valueOf("1"))
      JMoney.stopSpan()
      expectLogMessageContainingStrings(Seq("long-values", "the-long=1"))
    }
    "record propagate-able long values" in {
      JMoney.startSpan("parent")
      JMoney.record("the-long", java.lang.Long.valueOf("1"), true)
      JMoney.startSpan("child")
      JMoney.stopSpan()
      JMoney.stopSpan()
      expectLogMessageContainingStrings(Seq("parent", "the-long=1"))
      expectLogMessageContainingStrings(Seq("child", "the-long=1"))
    }
    "record boolean values" in {
      JMoney.startSpan("bool-values")
      JMoney.record("the-bool", true)
      JMoney.stopSpan()
      expectLogMessageContainingStrings(Seq("bool-values", "the-bool=true"))
    }
    "record propagate-able boolean values" in {
      JMoney.startSpan("parent")
      JMoney.record("the-bool", true, true)
      JMoney.startSpan("child")
      JMoney.stopSpan()
      JMoney.stopSpan()
      expectLogMessageContainingStrings(Seq("parent", "the-bool=true"))
      expectLogMessageContainingStrings(Seq("child", "the-bool=true"))
    }
    "record double values" in {
      JMoney.startSpan("double-values")
      JMoney.record("the-double", java.lang.Double.valueOf("3.14"))
      JMoney.stopSpan()
      expectLogMessageContainingStrings(Seq("double-values", "the-double=3.14"))
    }
    "record propagate-able double values" in {
      JMoney.startSpan("parent")
      JMoney.record("the-double", java.lang.Double.valueOf("3.14"), true)
      JMoney.startSpan("child")
      JMoney.stopSpan()
      JMoney.stopSpan()
      expectLogMessageContainingStrings(Seq("parent", "the-double=3.14"))
      expectLogMessageContainingStrings(Seq("child", "the-double=3.14"))
    }
    "record string values" in {
      JMoney.startSpan("string-values")
      JMoney.record("the-string", "yo")
      JMoney.stopSpan()
      expectLogMessageContainingStrings(Seq("string-values", "the-string=yo"))
    }
    "record propagate-able string values" in {
      JMoney.startSpan("parent")
      JMoney.record("the-string", "yo", true)
      JMoney.startSpan("child")
      JMoney.stopSpan()
      JMoney.stopSpan()
      expectLogMessageContainingStrings(Seq("parent", "the-string=yo"))
      expectLogMessageContainingStrings(Seq("child", "the-string=yo"))
    }
    "record using timers" in {
      JMoney.startSpan("timer")
      JMoney.startTimer("the-timer")
      JMoney.stopTimer("the-timer")
      JMoney.stopSpan()
      expectLogMessageContaining("the-timer")
    }
    "record a timestamp" in {
      JMoney.startSpan("stamp")
      JMoney.record("stamp-this")
      JMoney.stopSpan()
      expectLogMessageContaining("stamp-this")
    }
    "record a long as an Object" in {
      val lng: java.lang.Long = 100L;
      val obj: AnyRef = lng;
      JMoney.startSpan("lng")
      JMoney.record("long", obj)
      JMoney.stopSpan()
      expectLogMessageContaining("long=100")
    }
    "record a boolean as an Object" in {
      val boo: java.lang.Boolean = true;
      val obj: AnyRef = boo;
      JMoney.startSpan("boo")
      JMoney.record("bool", obj)
      JMoney.stopSpan()
      expectLogMessageContaining("bool=true")
    }
    "record a double as an Object" in {
      val dbl: java.lang.Double = 3.14;
      val obj: AnyRef = dbl;
      JMoney.startSpan("dbl")
      JMoney.record("double", obj)
      JMoney.stopSpan()
      expectLogMessageContaining("double=3.14")
    }
    "record a string as an Object" in {
      val str: java.lang.String = "hello";
      val obj: AnyRef = str;
      JMoney.startSpan("str")
      JMoney.record("string", obj)
      JMoney.stopSpan()
      expectLogMessageContaining("string=hello")
    }
    "record any object as an Object" in {
      val lst = List(1)
      JMoney.startSpan("lst")
      JMoney.record("list", lst)
      JMoney.stopSpan()
      expectLogMessageContaining("list=List(1)")
    }
    "record null as a string Note with None" in {
      val obj: AnyRef = null
      JMoney.startSpan("null")
      JMoney.record("nill", obj)
      JMoney.stopSpan()
      expectLogMessageContaining("nill=NULL")
    }
  }
}
