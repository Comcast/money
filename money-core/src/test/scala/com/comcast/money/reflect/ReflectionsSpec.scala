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

package com.comcast.money.reflect

import com.comcast.money.annotations.{Traced, TracedData}
import com.comcast.money.core._
import com.sun.istack.internal.NotNull
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, OneInstancePerTest, WordSpec }

class ReflectionsSpec extends WordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  val mockTracer: Tracer = mock[Tracer]

  val testReflections = new Reflections {
    val tracer: Tracer = mockTracer
  }

  val samples = new Samples()
  val clazz = samples.getClass
  val methodWithoutArguments = clazz.getMethod("methodWithoutArguments")
  val methodWithTracedData = clazz.getMethod(
    "methodWithTracedData", classOf[String], classOf[Long], classOf[Double], classOf[Boolean], classOf[Double]
  )
  val methodWithMultipleAnnotations = clazz.getMethod("methodWithMultipleAnnotations", classOf[String])
  val methodWithNoTracedDataArguments = clazz.getMethod("methodWithNoTracedDataArguments", classOf[String])
  val methodWithTracedDataPropagate = clazz.getMethod("methodWithTracedDataPropagate", classOf[String])

  "Caclulating result on failure" should {
    "return success if an exception matches a type in the ignored list" in {
      val matchingException = new IllegalArgumentException("ignored")
      val result = testReflections.exceptionMatches(matchingException, Array(classOf[IllegalArgumentException]))
      result shouldBe true
    }
    "return failure if an exception does not match a type in the ignored list" in {
      val matchingException = new RuntimeException("not-ignored")
      val result = testReflections.exceptionMatches(matchingException, Array(classOf[IllegalArgumentException]))
      result shouldBe false
    }
  }
  "Extracting Traced Data Annotations" should {
    "return an empty array if there are no arguments" in {
      val anns = testReflections.extractTracedDataAnnotations(methodWithoutArguments)
      anns shouldBe empty
    }
    "return the traced data annotation if present" in {
      val anns = testReflections.extractTracedDataAnnotations(methodWithTracedData)

      anns.size shouldBe 5
      anns(0).get.value shouldBe "STRING"
      anns(1).get.value shouldBe "LONG"
      anns(2).get.value shouldBe "DOUBLE"
      anns(3).get.value shouldBe "BOOLEAN"
      anns(4) shouldBe None
    }
    "return the traced data annotation if multiple annotations are present for the parameter" in {
      val anns = testReflections.extractTracedDataAnnotations(methodWithMultipleAnnotations)

      anns.size shouldBe 1
      anns(0).get.value shouldBe "STRING"
    }
    "return None for the argument if no traced data param is present but method has arguments" in {
      val anns = testReflections.extractTracedDataAnnotations(methodWithNoTracedDataArguments)

      anns.size shouldBe 1
      anns(0) shouldBe None
    }
    "return the propagate flag appropriately when set" in {
      val anns = testReflections.extractTracedDataAnnotations(methodWithTracedDataPropagate)

      anns(0).get.propagate shouldBe true
    }
  }
  "Extracting Traced Data Parameter Values" should {
    "return an empty sequence if there are no arguments" in {
      val tds = testReflections.extractTracedDataValues(methodWithoutArguments, Array.empty)

      tds shouldBe empty
    }
    "return the notes when multiple traced data params are present" in {
      val args: Array[AnyRef] = Array("str", Long.box(100L), Double.box(3.14), Boolean.box(true), Double.box(2.22))
      val tds = testReflections.extractTracedDataValues(methodWithTracedData, args)

      tds.size shouldBe 5

      val strNote = tds(0).get._1
      strNote shouldBe a[StringNote]
      strNote.value shouldBe Some("str")
      strNote.name shouldBe "STRING"
      tds(0).get._2 shouldBe false

      val lngNote = tds(1).get._1
      lngNote shouldBe a[LongNote]
      lngNote.value shouldBe Some(100L)
      lngNote.name shouldBe "LONG"
      tds(1).get._2 shouldBe false

      val dblNote = tds(2).get._1
      dblNote shouldBe a[DoubleNote]
      dblNote.value shouldBe Some(3.14)
      dblNote.name shouldBe "DOUBLE"
      tds(2).get._2 shouldBe false

      val boolNote = tds(3).get._1
      boolNote shouldBe a[BooleanNote]
      boolNote.value shouldBe Some(true)
      boolNote.name shouldBe "BOOLEAN"
      tds(3).get._2 shouldBe false

      tds(4) shouldBe None
    }
    "return notes with None values when nulls are passed in for arguments" in {
      val args: Array[AnyRef] = Array(null, null, null, null, Double.box(3.14))
      val tds = testReflections.extractTracedDataValues(methodWithTracedData, args)

      tds.size shouldBe 5

      val strNote = tds(0).get._1
      strNote.value shouldBe None
      strNote.name shouldBe "STRING"
      tds(0).get._2 shouldBe false

      val lngNote = tds(1).get._1
      lngNote.value shouldBe None
      lngNote.name shouldBe "LONG"
      tds(1).get._2 shouldBe false

      val dblNote = tds(2).get._1
      dblNote shouldBe a[DoubleNote]
      dblNote.value shouldBe None
      dblNote.name shouldBe "DOUBLE"
      tds(2).get._2 shouldBe false

      val boolNote = tds(3).get._1
      boolNote shouldBe a[BooleanNote]
      boolNote.value shouldBe None
      boolNote.name shouldBe "BOOLEAN"
      tds(3).get._2 shouldBe false

      tds(4) shouldBe None
    }
    "return notes for traced params that have other annotations present" in {
      val args: Array[AnyRef] = Array("str")
      val tds = testReflections.extractTracedDataValues(methodWithMultipleAnnotations, args)

      val strNote = tds(0).get._1
      strNote.value shouldBe Some("str")
      strNote.name shouldBe "STRING"
      tds(0).get._2 shouldBe false
    }
  }
  "Recording traced parameter values" should {
    "record nothing when method is called with no arguments" in {
      val args: Array[AnyRef] = Array.empty
      testReflections.recordTracedParameters(methodWithoutArguments, args, mockTracer)
      verifyZeroInteractions(mockTracer)
    }
    "record traced data parameters" in {
      val args: Array[AnyRef] = Array("str", Long.box(100L), Double.box(3.14), Boolean.box(true), Double.box(2.22))
      testReflections.recordTracedParameters(methodWithTracedData, args, mockTracer)

      val noteCaptor = ArgumentCaptor.forClass(classOf[Note[_]])
      val propCaptor = ArgumentCaptor.forClass(classOf[Boolean])
      verify(mockTracer, times(4)).record(noteCaptor.capture(), propCaptor.capture())

      val strNote = noteCaptor.getAllValues.get(0)
      val lngNote = noteCaptor.getAllValues.get(1)
      val dblNote = noteCaptor.getAllValues.get(2)
      val boolNote = noteCaptor.getAllValues.get(3)

      strNote.value shouldBe Some("str")
      strNote.name shouldBe "STRING"

      lngNote.value shouldBe Some(100L)
      lngNote.name shouldBe "LONG"

      dblNote.value shouldBe Some(3.14)
      dblNote.name shouldBe "DOUBLE"

      boolNote.value shouldBe Some(true)
      boolNote.name shouldBe "BOOLEAN"

      propCaptor.getAllValues.get(0) shouldBe false
      propCaptor.getAllValues.get(1) shouldBe false
      propCaptor.getAllValues.get(1) shouldBe false
      propCaptor.getAllValues.get(2) shouldBe false
    }
    "record None for traced data parameters that are null" in {
      val args: Array[AnyRef] = Array(null, null, null, null, Double.box(3.14))
      testReflections.recordTracedParameters(methodWithTracedData, args, mockTracer)

      val noteCaptor = ArgumentCaptor.forClass(classOf[Note[_]])
      val propCaptor = ArgumentCaptor.forClass(classOf[Boolean])
      verify(mockTracer, times(4)).record(noteCaptor.capture(), propCaptor.capture())

      val strNote = noteCaptor.getAllValues.get(0)
      val lngNote = noteCaptor.getAllValues.get(1)
      val dblNote = noteCaptor.getAllValues.get(2)
      val boolNote = noteCaptor.getAllValues.get(3)

      strNote.value shouldBe None
      strNote.name shouldBe "STRING"

      lngNote.value shouldBe None
      lngNote.name shouldBe "LONG"

      dblNote.value shouldBe None
      dblNote.name shouldBe "DOUBLE"

      boolNote.value shouldBe None
      boolNote.name shouldBe "BOOLEAN"

      propCaptor.getAllValues.get(0) shouldBe false
      propCaptor.getAllValues.get(1) shouldBe false
      propCaptor.getAllValues.get(2) shouldBe false
      propCaptor.getAllValues.get(3) shouldBe false
    }
    "record with propagate traced parameters flagged with propagate" in {
      val args: Array[AnyRef] = Array("str")
      testReflections.recordTracedParameters(methodWithTracedDataPropagate, args, mockTracer)

      val noteCaptor = ArgumentCaptor.forClass(classOf[Note[_]])
      val propCaptor = ArgumentCaptor.forClass(classOf[Boolean])
      verify(mockTracer, times(1)).record(noteCaptor.capture(), propCaptor.capture())

      val strNote = noteCaptor.getAllValues.get(0)
      val prop = propCaptor.getAllValues.get(0)

      strNote.value shouldBe Some("str")
      prop shouldBe true
    }
    "propagate even on null values" in {
      val args: Array[AnyRef] = Array(null)
      testReflections.recordTracedParameters(methodWithTracedDataPropagate, args, mockTracer)

      val noteCaptor = ArgumentCaptor.forClass(classOf[Note[_]])
      val propCaptor = ArgumentCaptor.forClass(classOf[Boolean])
      verify(mockTracer, times(1)).record(noteCaptor.capture(), propCaptor.capture())

      val strNote = noteCaptor.getAllValues.get(0)
      val prop = propCaptor.getAllValues.get(0)

      strNote.value shouldBe None
      prop shouldBe true
    }
  }

  class Samples {
    def methodWithoutArguments(): Unit = {}

    def methodWithTracedData(@TracedData("STRING") str: String, @TracedData("LONG") lng: Long,
      @TracedData("DOUBLE") dbl: Double, @TracedData("BOOLEAN") bln: Boolean, nn: Double): Unit = {}

    def methodWithMultipleAnnotations(@TracedData("STRING")@NotNull str: String): Unit = {}

    def methodWithNoTracedDataArguments(@NotNull str: String): Unit = {}

    def methodWithTracedDataPropagate(@TracedData(value = "STRING", propagate = true) str: String): Unit = {}
  }
}
