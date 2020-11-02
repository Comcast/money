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

package com.comcast.money.core.reflect

import com.comcast.money.annotations.TracedData
import com.comcast.money.api.Note
import com.comcast.money.core._
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.OneInstancePerTest

class ReflectionsSpec extends AnyWordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  val mockTracer: Tracer = mock[Tracer]

  val testReflections = new Reflections {
    val tracer: Tracer = mockTracer
  }

  val samples = new Samples()
  val clazz = samples.getClass
  val methodWithoutArguments = clazz.getMethod("methodWithoutArguments")
  val methodWithTracedData = clazz.getMethod(
    "methodWithTracedData", classOf[String], classOf[Long], classOf[Double], classOf[Boolean], classOf[Double])
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

      val strNote = tds(0).get
      strNote.value shouldBe "str"
      strNote.name shouldBe "STRING"
      strNote.isSticky shouldBe false

      val lngNote = tds(1).get
      lngNote.value shouldBe 100L
      lngNote.name shouldBe "LONG"
      lngNote.isSticky shouldBe false

      val dblNote = tds(2).get
      dblNote.value shouldBe 3.14
      dblNote.name shouldBe "DOUBLE"
      dblNote.isSticky shouldBe false

      val boolNote = tds(3).get
      boolNote.value shouldBe true
      boolNote.name shouldBe "BOOLEAN"
      boolNote.isSticky shouldBe false

      tds(4) shouldBe None
    }
    "return notes with None values when nulls are passed in for arguments" in {
      val args: Array[AnyRef] = Array(null, null, null, null, Double.box(3.14))
      val tds = testReflections.extractTracedDataValues(methodWithTracedData, args)

      tds.size shouldBe 5

      val strNote = tds(0).get.asInstanceOf[Note[String]]
      strNote.value shouldBe null
      strNote.name shouldBe "STRING"
      strNote.isSticky shouldBe false

      val lngNote = tds(1).get.asInstanceOf[Note[String]]
      lngNote.value shouldBe null
      lngNote.name shouldBe "LONG"
      lngNote.isSticky shouldBe false

      val dblNote = tds(2).get.asInstanceOf[Note[String]]
      dblNote.value shouldBe null
      dblNote.name shouldBe "DOUBLE"
      dblNote.isSticky shouldBe false

      val boolNote = tds(3).get.asInstanceOf[Note[String]]
      boolNote.value shouldBe null
      boolNote.name shouldBe "BOOLEAN"
      boolNote.isSticky shouldBe false

      tds(4) shouldBe None
    }
    "return notes for traced params that have other annotations present" in {
      val args: Array[AnyRef] = Array("str")
      val tds = testReflections.extractTracedDataValues(methodWithMultipleAnnotations, args)

      val strNote = tds(0).get
      strNote.value shouldBe "str"
      strNote.name shouldBe "STRING"
      strNote.isSticky shouldBe false
    }
  }
  "Recording traced parameter values" should {
    "record nothing when method is called with no arguments" in {
      val args: Array[AnyRef] = Array.empty
      testReflections.recordTracedParameters(methodWithoutArguments, args, mockTracer.record)
      verifyNoMoreInteractions(mockTracer)
    }
    "record traced data parameters" in {
      val args: Array[AnyRef] = Array("str", Long.box(100L), Double.box(3.14), Boolean.box(true), Double.box(2.22))
      testReflections.recordTracedParameters(methodWithTracedData, args, mockTracer.record)

      val noteCaptor = ArgumentCaptor.forClass(classOf[Note[_]])
      verify(mockTracer, times(4)).record(noteCaptor.capture())

      val strNote = noteCaptor.getAllValues.get(0)
      val lngNote = noteCaptor.getAllValues.get(1)
      val dblNote = noteCaptor.getAllValues.get(2)
      val boolNote = noteCaptor.getAllValues.get(3)

      strNote.value shouldBe "str"
      strNote.name shouldBe "STRING"
      strNote.isSticky shouldBe false

      lngNote.value shouldBe 100L
      lngNote.name shouldBe "LONG"
      lngNote.isSticky shouldBe false

      dblNote.value shouldBe 3.14
      dblNote.name shouldBe "DOUBLE"
      dblNote.isSticky shouldBe false

      boolNote.value shouldBe true
      boolNote.name shouldBe "BOOLEAN"
      boolNote.isSticky shouldBe false
    }
    "record null for traced data parameters that are null" in {
      val args: Array[AnyRef] = Array(null, null, null, null, Double.box(3.14))
      testReflections.recordTracedParameters(methodWithTracedData, args, mockTracer.record)

      val noteCaptor = ArgumentCaptor.forClass(classOf[Note[_]])
      verify(mockTracer, times(4)).record(noteCaptor.capture())

      val strNote = noteCaptor.getAllValues.get(0).asInstanceOf[Note[String]]
      val lngNote = noteCaptor.getAllValues.get(1).asInstanceOf[Note[String]]
      val dblNote = noteCaptor.getAllValues.get(2).asInstanceOf[Note[String]]
      val boolNote = noteCaptor.getAllValues.get(3).asInstanceOf[Note[String]]

      strNote.value shouldBe null
      strNote.name shouldBe "STRING"
      strNote.isSticky shouldBe false

      lngNote.value shouldBe null
      lngNote.name shouldBe "LONG"
      lngNote.isSticky shouldBe false

      dblNote.value shouldBe null
      dblNote.name shouldBe "DOUBLE"
      dblNote.isSticky shouldBe false

      boolNote.value shouldBe null
      boolNote.name shouldBe "BOOLEAN"
      boolNote.isSticky shouldBe false
    }
    "record with propagate traced parameters flagged with propagate" in {
      val args: Array[AnyRef] = Array("str")
      testReflections.recordTracedParameters(methodWithTracedDataPropagate, args, mockTracer.record)

      val noteCaptor = ArgumentCaptor.forClass(classOf[Note[_]])
      verify(mockTracer, times(1)).record(noteCaptor.capture())

      val strNote = noteCaptor.getAllValues.get(0)

      strNote.value shouldBe "str"
      strNote.isSticky shouldBe true
    }
    "propagate even on null values" in {
      val args: Array[AnyRef] = Array(null)
      testReflections.recordTracedParameters(methodWithTracedDataPropagate, args, mockTracer.record)

      val noteCaptor = ArgumentCaptor.forClass(classOf[Note[_]])
      verify(mockTracer, times(1)).record(noteCaptor.capture())

      val strNote = noteCaptor.getAllValues.get(0).asInstanceOf[Note[String]]

      strNote.value shouldBe null
      strNote.isSticky shouldBe true
    }
  }

  class Samples {
    def methodWithoutArguments(): Unit = {}

    def methodWithTracedData(@TracedData("STRING") str: String, @TracedData("LONG") lng: Long,
      @TracedData("DOUBLE") dbl: Double, @TracedData("BOOLEAN") bln: Boolean, nn: Double): Unit = {}

    def methodWithMultipleAnnotations(@TracedData("STRING")@CustomAnnotation str: String): Unit = {}

    def methodWithNoTracedDataArguments(@CustomAnnotation str: String): Unit = {}

    def methodWithTracedDataPropagate(@TracedData(value = "STRING", propagate = true) str: String): Unit = {}
  }
}
