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

package com.comcast.money.spring

import com.comcast.money.annotations.{ Traced, TracedData }
import com.comcast.money.api.Note
import com.sun.istack.internal.NotNull
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ BeforeAndAfterEach, Matchers, WordSpec }
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

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

  "Spring Tracing in scala" should {
    "record traced data parameters" in {
      val noteCaptor: ArgumentCaptor[Note[_]] = ArgumentCaptor.forClass(classOf[Note[_]])

      sampleScalaBean.doSomethingWithTracedParams("tp", true, 200L, 3.14)
      verify(springTracer, times(4)).record(noteCaptor.capture)

      val note: Note[String] = noteCaptor.getAllValues.get(0).asInstanceOf[Note[String]]
      note.name shouldBe "STRING"
      note.value shouldBe "tp"

      val boolNote: Note[java.lang.Boolean] = noteCaptor.getAllValues.get(1).asInstanceOf[Note[java.lang.Boolean]]
      boolNote.name shouldBe "BOOLEAN"
      boolNote.value shouldBe true

      val longNote: Note[Long] = noteCaptor.getAllValues.get(2).asInstanceOf[Note[Long]]
      longNote.name shouldBe "LONG"
      longNote.value shouldBe 200L

      val dblNote: Note[Double] = noteCaptor.getAllValues.get(3).asInstanceOf[Note[Double]]
      dblNote.name shouldBe "DOUBLE"
      dblNote.value shouldBe 3.14
    }
    "record null traced data parameters" in {
      val noteCaptor: ArgumentCaptor[Note[_]] = ArgumentCaptor.forClass(classOf[Note[_]])

      sampleScalaBean.doSomethingWithTracedJavaParams(null, null, null, null)
      verify(springTracer, times(4)).record(noteCaptor.capture)

      val strNote: Note[String] = noteCaptor.getAllValues.get(0).asInstanceOf[Note[String]]
      strNote.name shouldBe "STRING"
      strNote.value shouldBe null

      val boolNote: Note[String] = noteCaptor.getAllValues.get(1).asInstanceOf[Note[String]]
      boolNote.name shouldBe "BOOLEAN"
      boolNote.value shouldBe null

      val longNote: Note[String] = noteCaptor.getAllValues.get(2).asInstanceOf[Note[String]]
      longNote.name shouldBe "LONG"
      longNote.value shouldBe null

      val dblNote: Note[String] = noteCaptor.getAllValues.get(3).asInstanceOf[Note[String]]
      dblNote.name shouldBe "DOUBLE"
      dblNote.value shouldBe null
    }
    "record traced parameters when more than one annotation is present" in {
      val noteCaptor: ArgumentCaptor[Note[_]] = ArgumentCaptor.forClass(classOf[Note[_]])

      sampleScalaBean.doSomethingWithTracedParamsAndNonTracedParams("foo", "bar")
      verify(springTracer, times(1)).record(noteCaptor.capture)

      noteCaptor.getValue.name shouldBe "STRING"
      noteCaptor.getValue.value shouldBe "foo"
    }
    "record a string note of None if traced data annotation is on parameter of invalid type with null value" in {
      val noteCaptor: ArgumentCaptor[Note[_]] = ArgumentCaptor.forClass(classOf[Note[_]])

      sampleScalaBean.doSomethingWithIllegalTracedParams(null)
      verify(springTracer, times(1)).record(noteCaptor.capture)

      noteCaptor.getValue.asInstanceOf[Note[String]].value shouldBe null
    }
    "record a string note on a parameter of an unsupported type that is not null" in {
      val noteCaptor: ArgumentCaptor[Note[_]] = ArgumentCaptor.forClass(classOf[Note[_]])

      sampleScalaBean.doSomethingWithIllegalTracedParams(List(1))
      verify(springTracer, times(1)).record(noteCaptor.capture)

      noteCaptor.getValue.value shouldBe "List(1)"
    }
    "propagate traced data params" in {
      val noteCaptor: ArgumentCaptor[Note[_]] = ArgumentCaptor.forClass(classOf[Note[_]])
      val propCaptor: ArgumentCaptor[Boolean] = ArgumentCaptor.forClass(classOf[Boolean])

      sampleScalaBean.doSomethingWithTracedParamsPropagated("prop", "bar")
      verify(springTracer, times(1)).record(noteCaptor.capture)

      noteCaptor.getValue.name shouldBe "STRING"
      noteCaptor.getValue.value shouldBe "prop"
      noteCaptor.getValue.isSticky shouldBe true
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
    @TracedData("DOUBLE") dbl: Double) {

    return
  }

  @Traced("SampleTrace")
  def doSomethingWithTracedJavaParams(
    @TracedData("STRING") str: String,
    @TracedData("BOOLEAN") bool: java.lang.Boolean,
    @TracedData("LONG") lng: java.lang.Long,
    @TracedData("DOUBLE") dbl: java.lang.Double) {

    return
  }

  @Traced("SampleTrace")
  def doSomethingWithTracedParamsAndNonTracedParams(
    @TracedData("STRING")@NotNull str: String,
    @NotNull nn: String): Unit = {

    return
  }

  @Traced("SampleTrace")
  def doSomethingWithTracedParamsPropagated(
    @TracedData(value = "STRING", propagate = true)@NotNull str: String,
    @NotNull nn: String): Unit = {

    return
  }

  @Traced("SampleTrace")
  def doSomethingWithIllegalTracedParams(@TracedData("WHAT") lst: List[Byte]): Unit = {

    return
  }
}
