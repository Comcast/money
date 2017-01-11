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

package com.comcast.money.spring3

import com.comcast.money.annotations.{ Traced, TracedData }
import com.comcast.money.api.Tag
import com.comcast.money.core._
import com.sun.istack.internal.NotNull
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterEach, Matchers, WordSpec }
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.test.context.{ ContextConfiguration, TestContextManager }

@ContextConfiguration(Array("classpath:test-context.xml"))
class TracedMethodInterceptorScalaSpec extends WordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  @Autowired
  private var sampleScalaBean: SampleScalaBean = _

  @Autowired
  private var springTracer: SpringTracer = _

  new TestContextManager(classOf[TracedMethodInterceptorScalaSpec]).prepareTestInstance(this)

  override def afterEach = {
    reset(springTracer)
  }

  "Spring3 Tracing in scala" should {
    "record traced data parameters" in {
      val tagCaptor: ArgumentCaptor[Tag[_]] = ArgumentCaptor.forClass(classOf[Tag[_]])

      sampleScalaBean.doSomethingWithTracedParams("tp", true, 200L, 3.14)
      verify(springTracer, times(4)).record(tagCaptor.capture)

      val note: Tag[String] = tagCaptor.getAllValues.get(0).asInstanceOf[Tag[String]]
      note.name shouldBe "STRING"
      note.value shouldBe "tp"

      val boolNote: Tag[java.lang.Boolean] = tagCaptor.getAllValues.get(1).asInstanceOf[Tag[java.lang.Boolean]]
      boolNote.name shouldBe "BOOLEAN"
      boolNote.value shouldBe true

      val longNote: Tag[Long] = tagCaptor.getAllValues.get(2).asInstanceOf[Tag[Long]]
      longNote.name shouldBe "LONG"
      longNote.value shouldBe 200L

      val dblNote: Tag[Double] = tagCaptor.getAllValues.get(3).asInstanceOf[Tag[Double]]
      dblNote.name shouldBe "DOUBLE"
      dblNote.value shouldBe 3.14
    }
    "record null traced data parameters" in {
      val tagCaptor: ArgumentCaptor[Tag[_]] = ArgumentCaptor.forClass(classOf[Tag[_]])

      sampleScalaBean.doSomethingWithTracedJavaParams(null, null, null, null)
      verify(springTracer, times(4)).record(tagCaptor.capture)

      val strNote: Tag[String] = tagCaptor.getAllValues.get(0).asInstanceOf[Tag[String]]
      strNote.name shouldBe "STRING"
      strNote.value shouldBe null

      val boolNote: Tag[String] = tagCaptor.getAllValues.get(1).asInstanceOf[Tag[String]]
      boolNote.name shouldBe "BOOLEAN"
      boolNote.value shouldBe null

      val longNote: Tag[String] = tagCaptor.getAllValues.get(2).asInstanceOf[Tag[String]]
      longNote.name shouldBe "LONG"
      longNote.value shouldBe null

      val dblNote: Tag[String] = tagCaptor.getAllValues.get(3).asInstanceOf[Tag[String]]
      dblNote.name shouldBe "DOUBLE"
      dblNote.value shouldBe null
    }
    "record traced parameters when more than one annotation is present" in {
      val tagCaptor: ArgumentCaptor[Tag[_]] = ArgumentCaptor.forClass(classOf[Tag[_]])

      sampleScalaBean.doSomethingWithTracedParamsAndNonTracedParams("foo", "bar")
      verify(springTracer, times(1)).record(tagCaptor.capture)

      tagCaptor.getValue.name shouldBe "STRING"
      tagCaptor.getValue.value shouldBe "foo"
    }
    "record a string note of None if traced data annotation is on parameter of invalid type with null value" in {
      val tagCaptor: ArgumentCaptor[Tag[_]] = ArgumentCaptor.forClass(classOf[Tag[_]])

      sampleScalaBean.doSomethingWithIllegalTracedParams(null)
      verify(springTracer, times(1)).record(tagCaptor.capture)

      tagCaptor.getValue.asInstanceOf[Tag[String]].value shouldBe null
    }
    "record a string note on a parameter of an unsupported type that is not null" in {
      val tagCaptor: ArgumentCaptor[Tag[_]] = ArgumentCaptor.forClass(classOf[Tag[_]])

      sampleScalaBean.doSomethingWithIllegalTracedParams(List(1))
      verify(springTracer, times(1)).record(tagCaptor.capture)

      tagCaptor.getValue.value shouldBe "List(1)"
    }
    "propagate traced data params" in {
      val tagCaptor: ArgumentCaptor[Tag[_]] = ArgumentCaptor.forClass(classOf[Tag[_]])
      val propCaptor: ArgumentCaptor[Boolean] = ArgumentCaptor.forClass(classOf[Boolean])

      sampleScalaBean.doSomethingWithTracedParamsPropagated("prop", "bar")
      verify(springTracer, times(1)).record(tagCaptor.capture)

      tagCaptor.getValue.name shouldBe "STRING"
      tagCaptor.getValue.value shouldBe "prop"
      tagCaptor.getValue.isSticky shouldBe true
    }
  }
}

@Component
class SampleScalaBean {

  @Autowired
  private var springTracer: SpringTracer = _

  @Traced("SampleTrace") def doSomethingGood {
    springTracer.record("foo", "bar")
  }

  @Traced("SampleTrace") def doSomethingBad {
    springTracer.record("foo", "bar")
    throw new IllegalStateException("fail")
  }

  @Traced("SampleTrace")
  def doSomethingWithTracedParams(
    @TracedData("STRING") str: String,
    @TracedData("BOOLEAN") bool: Boolean,
    @TracedData("LONG") lng: Long,
    @TracedData("DOUBLE") dbl: Double
  ) {

    return
  }

  @Traced("SampleTrace")
  def doSomethingWithTracedJavaParams(
    @TracedData("STRING") str: String,
    @TracedData("BOOLEAN") bool: java.lang.Boolean,
    @TracedData("LONG") lng: java.lang.Long,
    @TracedData("DOUBLE") dbl: java.lang.Double
  ) {

    return
  }

  @Traced("SampleTrace")
  def doSomethingWithTracedParamsAndNonTracedParams(
    @TracedData("STRING")@NotNull str: String,
    @NotNull nn: String
  ): Unit = {

    return
  }

  @Traced("SampleTrace")
  def doSomethingWithTracedParamsPropagated(
    @TracedData(value = "STRING", propagate = true)@NotNull str: String,
    @NotNull nn: String
  ): Unit = {

    return
  }

  @Traced("SampleTrace")
  def doSomethingWithIllegalTracedParams(@TracedData("WHAT") lst: List[Byte]): Unit = {

    return
  }
}
