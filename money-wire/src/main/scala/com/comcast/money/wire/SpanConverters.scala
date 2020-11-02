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

package com.comcast.money.wire

import java.io.ByteArrayOutputStream
import java.util
import java.util.Collections
import java.util.concurrent.TimeUnit

import com.comcast.money.api
import com.comcast.money.api.{ InstrumentationLibrary, Note, SpanId, SpanInfo }
import com.comcast.money.core._
import com.comcast.money.wire.avro
import com.comcast.money.wire.avro.NoteType
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.{ DeserializationFeature, ObjectMapper }
import io.opentelemetry.trace.{ Span, StatusCanonicalCode, TraceFlags, TraceState }
import org.apache.avro.Schema
import org.apache.avro.io.{ DecoderFactory, EncoderFactory }
import org.apache.avro.specific.{ SpecificDatumReader, SpecificDatumWriter }

import scala.collection.JavaConverters._

trait TypeConverter[From, To] {

  def convert(from: From): To
}

object TypeConverter {

  def instance[From, To](f: From => To): TypeConverter[From, To] = new TypeConverter[From, To] {
    def convert(from: From): To = f(from)
  }
}

object AvroConversions extends SpanAvroConverters {

  implicit class AvroConversionExtensions[A](val a: A) extends AnyVal {
    def convertTo[B](implicit tc: TypeConverter[A, B]): B = tc.convert(a)
  }
}
object JsonConversions extends SpanJsonConverters {

  implicit class JsonConversionExtensions[A](val a: A) extends AnyVal {
    def convertTo[B](implicit tc: TypeConverter[A, B]): B = tc.convert(a)
  }
}

trait SpanWireConverters {

  implicit val noteToWire: TypeConverter[api.Note[_], avro.Note] = TypeConverter.instance { from: api.Note[_] =>
    def avroNote(noteValue: avro.NoteValue): avro.Note = new avro.Note(from.name, from.timestamp, noteValue)

    from.value match {
      case l: Long => avroNote(
        new avro.NoteValue(avro.NoteType.Long, l.toString))
      case s: String => avroNote(
        new avro.NoteValue(avro.NoteType.String, s))
      case b: java.lang.Boolean => avroNote(
        new avro.NoteValue(avro.NoteType.Boolean, b.toString))
      case d: Double => avroNote(
        new avro.NoteValue(avro.NoteType.Double, d.toString))
      case null => avroNote(
        new avro.NoteValue(avro.NoteType.String, null))

    }
  }

  implicit val wireToNote: TypeConverter[avro.Note, api.Note[_]] = TypeConverter.instance { from: avro.Note =>
    def toOption[T](str: String)(ft: String => T): Option[T] = {
      if (str == null)
        None: Option[T]
      else
        Some(ft(str))
    }

    from.getValue.getType match {
      case NoteType.Boolean => api.Note.of(
        from.getName, from.getValue.getData.toBoolean, from.getTimestamp)
      case NoteType.Long => api.Note.of(from.getName, from.getValue.getData.toLong, from.getTimestamp)
      case NoteType.String => api.Note.of(
        from.getName, from.getValue.getData, from.getTimestamp)
      case NoteType.Double => api.Note.of(
        from.getName, from.getValue.getData.toDouble, from.getTimestamp)
    }
  }

  implicit val spanIdToWire: TypeConverter[api.SpanId, avro.SpanId] = TypeConverter.instance { spanId =>
    new avro.SpanId(spanId.traceId, spanId.parentId, spanId.selfId)
  }

  implicit val wireToSpanId: TypeConverter[avro.SpanId, api.SpanId] = TypeConverter.instance { spanId =>
    api.SpanId.createRemote(spanId.getTraceId, spanId.getParentId, spanId.getSpanId, TraceFlags.getSampled, TraceState.getDefault)
  }

  implicit val spanToWire: TypeConverter[SpanInfo, avro.Span] = TypeConverter.instance { span: SpanInfo =>

    var success = span.success
    new avro.Span(
      span.name,
      span.appName,
      span.host,
      span.library.name,
      span.library.version,
      span.durationMicros,
      if (success == null) true else success,
      span.startTimeMillis,
      implicitly[TypeConverter[api.SpanId, avro.SpanId]].convert(span.id),
      span.notes.values.asScala.toList.map(implicitly[TypeConverter[api.Note[_], avro.Note]].convert).asJava)
  }

  implicit val wireToSpan: TypeConverter[avro.Span, SpanInfo] = TypeConverter.instance { from: avro.Span =>

    def toNotesMap(notes: java.util.List[avro.Note]): java.util.Map[String, api.Note[_]] = {
      val res = new java.util.HashMap[String, api.Note[_]]
      notes.asScala.foreach(n => res.put(n.getName, implicitly[TypeConverter[avro.Note, api.Note[_]]].convert(n)))
      res
    }

    def toInstrumentationLibrary(span: avro.Span): InstrumentationLibrary =
      if (span.getLibraryName != null && !span.getLibraryName.isEmpty) {
        new InstrumentationLibrary(span.getLibraryName, span.getLibraryVersion)
      } else {
        InstrumentationLibrary.UNKNOWN
      }

    new SpanInfo {
      override def notes(): util.Map[String, Note[_]] = toNotesMap(from.getNotes)
      override def events(): util.List[SpanInfo.Event] = Collections.emptyList()
      override def startTimeNanos(): Long = TimeUnit.MILLISECONDS.toNanos(from.getStartTime)
      override def endTimeNanos(): Long = startTimeNanos + durationNanos
      override def status(): StatusCanonicalCode = if (from.getSuccess) StatusCanonicalCode.OK else StatusCanonicalCode.ERROR
      override def kind(): Span.Kind = Span.Kind.INTERNAL
      override def description(): String = ""
      override def id(): SpanId = implicitly[TypeConverter[avro.SpanId, api.SpanId]].convert(from.getId)
      override def name(): String = from.getName
      override def durationNanos(): Long = TimeUnit.MICROSECONDS.toNanos(from.getDuration)
      override def library(): InstrumentationLibrary = toInstrumentationLibrary(from)
      override def appName(): String = from.getAppName
      override def host(): String = from.getHost
    }
  }
}

trait SpanAvroConverters extends SpanWireConverters {

  val spanDatumWriter = new SpecificDatumWriter[avro.Span](avro.Span.getClassSchema)
  val spanDatumReader = new SpecificDatumReader[avro.Span](avro.Span.getClassSchema)

  implicit val spanToAvro: TypeConverter[SpanInfo, Array[Byte]] = TypeConverter.instance { span =>

    val bytes = new ByteArrayOutputStream()
    val spanBinaryEncoder = EncoderFactory.get.directBinaryEncoder(bytes, null)
    val wireSpan = implicitly[TypeConverter[SpanInfo, avro.Span]].convert(span)
    spanDatumWriter.write(wireSpan, spanBinaryEncoder)
    bytes.toByteArray
  }

  implicit val avroToSpan: TypeConverter[Array[Byte], SpanInfo] = TypeConverter.instance { bytes =>
    val spanBinaryDecoder = DecoderFactory.get.binaryDecoder(bytes, 0, bytes.length, null)
    implicitly[TypeConverter[avro.Span, SpanInfo]].convert(spanDatumReader.read(null, spanBinaryDecoder))
  }
}

trait SpanJsonConverters extends SpanWireConverters {

  val mapper: ObjectMapper = createSpanJsonMapper()

  /**
   * Mixin that is used by the Jackson ObjectMapper so we can explicitly ignore certain properties
   */
  abstract class IgnoreSpanProperties {
    @JsonIgnore
    def getSchema(): Schema
  }

  def createSpanJsonMapper(): ObjectMapper = {
    // Make sure we don't fail on unknown types
    val jsonMapper: ObjectMapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    jsonMapper.setMixIns(
      Map[Class[_], Class[_]](
        classOf[avro.Span] -> classOf[IgnoreSpanProperties],
        classOf[avro.SpanId] -> classOf[IgnoreSpanProperties],
        classOf[avro.Note] -> classOf[IgnoreSpanProperties],
        classOf[avro.NoteValue] -> classOf[IgnoreSpanProperties]).asJava)
    jsonMapper
  }

  implicit val spanToJson: TypeConverter[SpanInfo, String] = TypeConverter.instance { span =>
    mapper.writeValueAsString(implicitly[TypeConverter[SpanInfo, avro.Span]].convert(span))
  }

  implicit val jsonToSpan: TypeConverter[String, SpanInfo] = TypeConverter.instance { str =>
    implicitly[TypeConverter[avro.Span, SpanInfo]].convert(mapper.readValue(str, classOf[avro.Span]))
  }
}
