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

package com.comcast.money.wire

import java.io.ByteArrayOutputStream

import com.comcast.money.api
import com.comcast.money.api.{ SpanInfo, Tag }
import com.comcast.money.core._
import com.comcast.money.wire.avro
import com.comcast.money.wire.avro.TagType
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.{ DeserializationFeature, ObjectMapper }
import org.apache.avro.Schema
import org.apache.avro.io.{ DecoderFactory, EncoderFactory }
import org.apache.avro.specific.{ SpecificDatumReader, SpecificDatumWriter }

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

  import scala.collection.JavaConversions._

  implicit val tagToWire: TypeConverter[Tag[_], avro.Tag] = TypeConverter.instance { from: Tag[_] =>
    def avroTag(tagValue: avro.TagValue): avro.Tag = new avro.Tag(from.name, from.timestamp, tagValue)

    from.value match {
      case l: Long => avroTag(
        new avro.TagValue(avro.TagType.Long, l.toString)
      )
      case s: String => avroTag(
        new avro.TagValue(avro.TagType.String, s)
      )
      case b: java.lang.Boolean => avroTag(
        new avro.TagValue(avro.TagType.Boolean, b.toString)
      )
      case d: Double => avroTag(
        new avro.TagValue(avro.TagType.Double, d.toString)
      )
      case null => avroTag(
        new avro.TagValue(avro.TagType.String, null)
      )

    }
  }

  implicit val wireToTag: TypeConverter[avro.Tag, Tag[_]] = TypeConverter.instance { from: avro.Tag =>
    def toOption[T](str: String)(ft: String => T): Option[T] = {
      if (str == null)
        None: Option[T]
      else
        Some(ft(str))
    }

    from.getValue.getType match {
      case TagType.Boolean => api.Tag.of(
        from.getName, from.getValue.getData.toBoolean, from.getTimestamp
      )
      case TagType.Long => api.Tag.of(from.getName, from.getValue.getData.toLong, from.getTimestamp)
      case TagType.String => api.Tag.of(
        from.getName, from.getValue.getData, from.getTimestamp
      )
      case TagType.Double => api.Tag.of(
        from.getName, from.getValue.getData.toDouble, from.getTimestamp
      )
    }
  }

  implicit val spanIdToWire: TypeConverter[api.SpanId, avro.SpanId] = TypeConverter.instance { spanId =>
    new avro.SpanId(spanId.traceId, spanId.parentId, spanId.selfId)
  }

  implicit val wireToSpanId: TypeConverter[avro.SpanId, api.SpanId] = TypeConverter.instance { spanId =>
    new api.SpanId(spanId.getTraceId, spanId.getParentId, spanId.getSpanId)
  }

  implicit val spanToWire: TypeConverter[SpanInfo, avro.Span] = TypeConverter.instance { span: SpanInfo =>

    new avro.Span(
      span.name,
      span.appName,
      span.host,
      span.durationMicros,
      span.success,
      span.startTimeMillis,
      implicitly[TypeConverter[api.SpanId, avro.SpanId]].convert(span.id),
      span.tags.values.toList.map(implicitly[TypeConverter[Tag[_], avro.Tag]].convert)
    )
  }

  implicit val wireToSpan: TypeConverter[avro.Span, SpanInfo] = TypeConverter.instance { from: avro.Span =>

    def toTagsMap(tags: java.util.List[avro.Tag]): java.util.Map[String, Tag[_]] = {
      val res = new java.util.HashMap[String, Tag[_]]
      tags.foreach(n => res.put(n.getName, implicitly[TypeConverter[avro.Tag, Tag[_]]].convert(n)))
      res
    }

    CoreSpanInfo(
      id = implicitly[TypeConverter[avro.SpanId, api.SpanId]].convert(from.getId),
      name = from.getName,
      appName = from.getAppName,
      host = from.getHost,
      startTimeMillis = from.getStartTime,
      success = from.getSuccess,
      durationMicros = from.getDuration,
      tags = toTagsMap(from.getTags)
    )
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

    // We need to ignore the getSchema Javabean Properties or else serdes will fail
    jsonMapper.addMixInAnnotations(classOf[avro.Span], classOf[IgnoreSpanProperties]);
    jsonMapper.addMixInAnnotations(classOf[avro.SpanId], classOf[IgnoreSpanProperties]);
    jsonMapper.addMixInAnnotations(classOf[avro.Tag], classOf[IgnoreSpanProperties]);
    jsonMapper.addMixInAnnotations(classOf[avro.TagValue], classOf[IgnoreSpanProperties]);

    jsonMapper
  }

  implicit val spanToJson: TypeConverter[SpanInfo, String] = TypeConverter.instance { span =>
    mapper.writeValueAsString(implicitly[TypeConverter[SpanInfo, avro.Span]].convert(span))
  }

  implicit val jsonToSpan: TypeConverter[String, SpanInfo] = TypeConverter.instance { str =>
    implicitly[TypeConverter[avro.Span, SpanInfo]].convert(mapper.readValue(str, classOf[avro.Span]))
  }
}
