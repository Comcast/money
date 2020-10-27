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

import java.lang.reflect.AccessibleObject

import com.comcast.money.annotations.{Traced, TracedData}
import com.comcast.money.api.{Note, Span}
import com.sun.istack.internal.NotNull
import io.opentelemetry.context.Scope
import org.aopalliance.intercept.MethodInvocation
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.{Bean, Configuration, EnableAspectJAutoProxy}
import org.springframework.stereotype.Component
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.{ContextConfiguration, TestContextManager}

@RunWith(classOf[SpringRunner])
@ContextConfiguration(classes = Array(classOf[TestConfig]))
class TracedMethodInterceptorScalaSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  @Autowired
  private var sampleScalaBean: SampleScalaBean = _

  @MockBean
  private var springTracer: SpringTracer = _

  private var spanBuilder: Span.Builder = _
  private var span: Span = _
  private var scope: Scope = _

  new TestContextManager(classOf[TracedMethodInterceptorScalaSpec]).prepareTestInstance(this)

  override def beforeEach: Unit = {
    spanBuilder = mock[Span.Builder]
    span = mock[Span]
    scope = mock[Scope]

    when(springTracer.spanBuilder(any())).thenReturn(spanBuilder)
    when(spanBuilder.record(any())).thenReturn(spanBuilder)
    when(spanBuilder.startSpan()).thenReturn(span)
    when(springTracer.withSpan(span)).thenReturn(scope)
  }

  override def afterEach: Unit = {
    reset(springTracer)
  }

  "Spring Tracing in scala" should {
    "record traced data parameters" in {
      val noteCaptor = ArgumentCaptor.forClass(classOf[Note[_]])

      sampleScalaBean.doSomethingWithTracedParams("tp", true, 200L, 3.14)
      verify(spanBuilder, times(4)).record(noteCaptor.capture)

      val note = noteCaptor.getAllValues.get(0).asInstanceOf[Note[String]]
      note.name shouldBe "STRING"
      note.value shouldBe "tp"

      val boolNote = noteCaptor.getAllValues.get(1).asInstanceOf[Note[java.lang.Boolean]]
      boolNote.name shouldBe "BOOLEAN"
      boolNote.value shouldBe true

      val longNote = noteCaptor.getAllValues.get(2).asInstanceOf[Note[Long]]
      longNote.name shouldBe "LONG"
      longNote.value shouldBe 200L

      val dblNote = noteCaptor.getAllValues.get(3).asInstanceOf[Note[Double]]
      dblNote.name shouldBe "DOUBLE"
      dblNote.value shouldBe 3.14
    }
    "record null traced data parameters" in {
      val noteCaptor = ArgumentCaptor.forClass(classOf[Note[_]])

      sampleScalaBean.doSomethingWithTracedJavaParams(null, null, null, null)
      verify(spanBuilder, times(4)).record(noteCaptor.capture)

      val strNote = noteCaptor.getAllValues.get(0).asInstanceOf[Note[String]]
      strNote.name shouldBe "STRING"
      strNote.value shouldBe null

      val boolNote = noteCaptor.getAllValues.get(1).asInstanceOf[Note[String]]
      boolNote.name shouldBe "BOOLEAN"
      boolNote.value shouldBe null

      val longNote = noteCaptor.getAllValues.get(2).asInstanceOf[Note[String]]
      longNote.name shouldBe "LONG"
      longNote.value shouldBe null

      val dblNote = noteCaptor.getAllValues.get(3).asInstanceOf[Note[String]]
      dblNote.name shouldBe "DOUBLE"
      dblNote.value shouldBe null
    }
    "record traced parameters when more than one annotation is present" in {
      val noteCaptor = ArgumentCaptor.forClass(classOf[Note[_]])

      sampleScalaBean.doSomethingWithTracedParamsAndNonTracedParams("foo", "bar")
      verify(spanBuilder, times(1)).record(noteCaptor.capture)

      noteCaptor.getValue.name shouldBe "STRING"
      noteCaptor.getValue.value shouldBe "foo"
    }
    "record a string note of None if traced data annotation is on parameter of invalid type with null value" in {
      val noteCaptor = ArgumentCaptor.forClass(classOf[Note[_]])

      sampleScalaBean.doSomethingWithIllegalTracedParams(null)
      verify(spanBuilder, times(1)).record(noteCaptor.capture)

      noteCaptor.getValue.asInstanceOf[Note[String]].value shouldBe null
    }
    "record a string note on a parameter of an unsupported type that is not null" in {
      val noteCaptor = ArgumentCaptor.forClass(classOf[Note[_]])

      sampleScalaBean.doSomethingWithIllegalTracedParams(List(1))
      verify(spanBuilder, times(1)).record(noteCaptor.capture)

      noteCaptor.getValue.value shouldBe "List(1)"
    }
    "propagate traced data params" in {
      val noteCaptor = ArgumentCaptor.forClass(classOf[Note[_]])
      val propCaptor = ArgumentCaptor.forClass(classOf[Boolean])

      sampleScalaBean.doSomethingWithTracedParamsPropagated("prop", "bar")
      verify(spanBuilder, times(1)).record(noteCaptor.capture)

      noteCaptor.getValue.name shouldBe "STRING"
      noteCaptor.getValue.value shouldBe "prop"
      noteCaptor.getValue.isSticky shouldBe true
    }
    "should invoke joinpoint directly without Traced annotation" in {
      val tracer = new SpringTracer()
      val interceptor = new TracedMethodInterceptor(tracer)
      val invocation = mock[MethodInvocation]
      val accessibleObject = mock[AccessibleObject]
      when(invocation.getStaticPart).thenReturn(accessibleObject)
      when(accessibleObject.getAnnotation(classOf[Traced])).thenReturn(null)

      interceptor.invoke(invocation)

      verify(invocation, never()).getMethod
      verify(invocation, never()).getArguments
      verify(invocation).proceed()
    }
  }
}

@Component
class SampleScalaBean {

  @Autowired
  private var springTracer: SpringTracer = _

  @Traced("SampleTrace") def doSomethingGood(): Unit = {
    springTracer.record("foo", "bar")
  }

  @Traced("SampleTrace") def doSomethingBad(): Unit = {
    springTracer.record("foo", "bar")
    throw new IllegalStateException("fail")
  }

  @Traced("SampleTrace")
  def doSomethingWithTracedParams(
    @TracedData("STRING") str: String,
    @TracedData("BOOLEAN") bool: Boolean,
    @TracedData("LONG") lng: Long,
    @TracedData("DOUBLE") dbl: Double): Unit = ()

  @Traced("SampleTrace")
  def doSomethingWithTracedJavaParams(
    @TracedData("STRING") str: String,
    @TracedData("BOOLEAN") bool: java.lang.Boolean,
    @TracedData("LONG") lng: java.lang.Long,
    @TracedData("DOUBLE") dbl: java.lang.Double): Unit = ()

  @Traced("SampleTrace")
  def doSomethingWithTracedParamsAndNonTracedParams(
    @TracedData("STRING")@NotNull str: String,
    @NotNull nn: String): Unit = ()

  @Traced("SampleTrace")
  def doSomethingWithTracedParamsPropagated(
    @TracedData(value = "STRING", propagate = true)@NotNull str: String,
    @NotNull nn: String): Unit = ()

  @Traced("SampleTrace")
  def doSomethingWithIllegalTracedParams(@TracedData("WHAT") lst: List[Byte]): Unit = ()
}

@Configuration
@EnableAspectJAutoProxy
class TestConfig {
  @Bean
  def tracedMethodInterceptor(springTracer: SpringTracer) = new TracedMethodInterceptor(springTracer)

  @Bean
  def tracedMethodAdvisor(tracedMethodInterceptor: TracedMethodInterceptor) = new TracedMethodAdvisor(tracedMethodInterceptor)

  @Bean
  def sampleScalaBean(): SampleScalaBean = new SampleScalaBean
}