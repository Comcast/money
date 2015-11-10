package com.comcast.money.spring3

import com.comcast.money.annotations.{Traced, TracedData}
import com.comcast.money.core._
import com.sun.istack.internal.NotNull
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.test.context.{ContextConfiguration, TestContextManager}

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
      val noteCaptor: ArgumentCaptor[Note[_]] = ArgumentCaptor.forClass(classOf[Note[_]])

      sampleScalaBean.doSomethingWithTracedParams("tp", true, 200L, 3.14)
      verify(springTracer, times(4)).record(noteCaptor.capture, anyBoolean())

      val stringNote: StringNote = noteCaptor.getAllValues.get(0).asInstanceOf[StringNote]
      stringNote.name shouldBe "STRING"
      stringNote.value.get shouldBe "tp"

      val booleanNote: BooleanNote = noteCaptor.getAllValues.get(1).asInstanceOf[BooleanNote]
      booleanNote.name shouldBe "BOOLEAN"
      booleanNote.value.get shouldBe true

      val longNote: LongNote = noteCaptor.getAllValues.get(2).asInstanceOf[LongNote]
      longNote.name shouldBe "LONG"
      longNote.value.get shouldBe 200L

      val doubleNote: DoubleNote = noteCaptor.getAllValues.get(3).asInstanceOf[DoubleNote]
      doubleNote.name shouldBe "DOUBLE"
      doubleNote.value.get shouldBe 3.14
    }
    "record null traced data parameters" in {
      val noteCaptor: ArgumentCaptor[Note[_]] = ArgumentCaptor.forClass(classOf[Note[_]])

      sampleScalaBean.doSomethingWithTracedJavaParams(null, null, null, null)
      verify(springTracer, times(4)).record(noteCaptor.capture, anyBoolean())

      val stringNote: StringNote = noteCaptor.getAllValues.get(0).asInstanceOf[StringNote]
      stringNote.name shouldBe "STRING"
      stringNote.value.isEmpty shouldBe true

      val booleanNote: BooleanNote = noteCaptor.getAllValues.get(1).asInstanceOf[BooleanNote]
      booleanNote.name shouldBe "BOOLEAN"
      booleanNote.value.isEmpty shouldBe true

      val longNote: LongNote = noteCaptor.getAllValues.get(2).asInstanceOf[LongNote]
      longNote.name shouldBe "LONG"
      longNote.value.isEmpty shouldBe true

      val doubleNote: DoubleNote = noteCaptor.getAllValues.get(3).asInstanceOf[DoubleNote]
      doubleNote.name shouldBe "DOUBLE"
      doubleNote.value.isEmpty shouldBe true
    }
    "record traced parameters when more than one annotation is present" in {
      val noteCaptor: ArgumentCaptor[Note[_]] = ArgumentCaptor.forClass(classOf[Note[_]])

      sampleScalaBean.doSomethingWithTracedParamsAndNonTracedParams("foo", "bar")
      verify(springTracer, times(1)).record(noteCaptor.capture, anyBoolean())

      noteCaptor.getValue.name shouldBe "STRING"
      noteCaptor.getValue.value.get shouldBe "foo"
    }
    "record a string note of None if traced data annotation is on parameter of invalid type with null value" in {
      val noteCaptor: ArgumentCaptor[Note[_]] = ArgumentCaptor.forClass(classOf[Note[_]])

      sampleScalaBean.doSomethingWithIllegalTracedParams(null)
      verify(springTracer, times(1)).record(noteCaptor.capture, anyBoolean())

      noteCaptor.getValue.value shouldBe None
    }
    "record a string note on a parameter of an unsupported type that is not null" in {
      val noteCaptor: ArgumentCaptor[Note[_]] = ArgumentCaptor.forClass(classOf[Note[_]])

      sampleScalaBean.doSomethingWithIllegalTracedParams(List(1))
      verify(springTracer, times(1)).record(noteCaptor.capture, anyBoolean())

      noteCaptor.getValue.value.get shouldBe "List(1)"
    }
    "propagate traced data params" in {
      val noteCaptor: ArgumentCaptor[Note[_]] = ArgumentCaptor.forClass(classOf[Note[_]])
      val propCaptor: ArgumentCaptor[Boolean] = ArgumentCaptor.forClass(classOf[Boolean])

      sampleScalaBean.doSomethingWithTracedParamsPropagated("prop", "bar")
      verify(springTracer, times(1)).record(noteCaptor.capture, propCaptor.capture)

      noteCaptor.getValue.name shouldBe "STRING"
      noteCaptor.getValue.value.get shouldBe "prop"
      propCaptor.getValue shouldBe true
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
  def doSomethingWithTracedParamsAndNonTracedParams(@TracedData("STRING") @NotNull str: String,
    @NotNull nn: String): Unit = {

    return
  }

  @Traced("SampleTrace")
  def doSomethingWithTracedParamsPropagated(@TracedData(value = "STRING", propagate = true) @NotNull str: String,
    @NotNull nn: String): Unit = {

    return
  }

  @Traced("SampleTrace")
  def doSomethingWithIllegalTracedParams(@TracedData("WHAT") lst: List[Byte]): Unit = {

    return
  }
}
