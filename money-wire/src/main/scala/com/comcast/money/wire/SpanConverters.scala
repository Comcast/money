package com.comcast.money.wire

import java.io.ByteArrayOutputStream

import com.comcast.money.core._
import com.comcast.money.wire.avro
import com.comcast.money.wire.avro.NoteType
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import org.apache.avro.Schema
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter}

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

  implicit val noteToWire: TypeConverter[Note[_], avro.Note] = TypeConverter.instance { from: Note[_] =>
    def avroNote(noteValue: avro.NoteValue): avro.Note = new avro.Note(from.name, from.timestamp, noteValue)

    from match {
      case LongNote(name, value, timestamp) => avroNote(
        new avro.NoteValue(avro.NoteType.Long, value.map(_.toString).getOrElse(null)))
      case StringNote(name, value, timestamp) => avroNote(
        new avro.NoteValue(avro.NoteType.String, value.getOrElse(null)))
      case BooleanNote(name, value, timestamp) => avroNote(
        new avro.NoteValue(avro.NoteType.Boolean, value.map(_.toString).getOrElse(null)))
      case DoubleNote(name, value, timestamp) => avroNote(
        new avro.NoteValue(avro.NoteType.Double, value.map(_.toString).getOrElse(null)))
    }
  }

  implicit val wireToNote: TypeConverter[avro.Note, Note[_]] = TypeConverter.instance { from: avro.Note =>
    def toOption[T](str: String)(ft: String => T): Option[T] = {
      if (str == null)
        None: Option[T]
      else
        Some(ft(str))
    }

    from.getValue.getType match {
      case NoteType.Boolean => BooleanNote(
        from.getName, toOption[Boolean](from.getValue.getData)(_.toBoolean), from.getTimestamp)
      case NoteType.Long => LongNote(from.getName, toOption[Long](from.getValue.getData)(_.toLong), from.getTimestamp)
      case NoteType.String => StringNote(
        from.getName, toOption[String](from.getValue.getData)(_.toString), from.getTimestamp)
      case NoteType.Double => DoubleNote(
        from.getName, toOption[Double](from.getValue.getData)(_.toDouble), from.getTimestamp)
    }
  }

  implicit val spanIdToWire: TypeConverter[SpanId, avro.SpanId] = TypeConverter.instance { spanId =>
    new avro.SpanId(spanId.traceId, spanId.parentSpanId, spanId.spanId)
  }

  implicit val wireToSpanId: TypeConverter[avro.SpanId, SpanId] = TypeConverter.instance { spanId =>
    SpanId(spanId.getTraceId, spanId.getParentId, spanId.getSpanId)
  }

  implicit val spanToWire: TypeConverter[Span, avro.Span] = TypeConverter.instance { span: Span =>

    new avro.Span(
      span.spanName,
      span.appName,
      span.host,
      span.duration,
      span.success,
      span.startTime,
      implicitly[TypeConverter[SpanId, avro.SpanId]].convert(span.spanId),
      span.notes.values.toList.map(implicitly[TypeConverter[Note[_], avro.Note]].convert))
  }

  implicit val wireToSpan: TypeConverter[avro.Span, Span] = TypeConverter.instance { from: avro.Span =>
    Span(
      spanId = implicitly[TypeConverter[avro.SpanId, SpanId]].convert(from.getId),
      spanName = from.getName,
      appName = from.getAppName,
      host = from.getHost,
      startTime = from.getStartTime,
      success = from.getSuccess,
      duration = from.getDuration,
      notes = (for (note: avro.Note <- from.getNotes) yield note
        .getName -> implicitly[TypeConverter[avro.Note, Note[_]]].convert(note)).toMap
    )
  }
}

trait SpanAvroConverters extends SpanWireConverters {

  val spanDatumWriter = new SpecificDatumWriter[avro.Span](avro.Span.getClassSchema)
  val spanDatumReader = new SpecificDatumReader[avro.Span](avro.Span.getClassSchema)

  implicit val spanToAvro: TypeConverter[Span, Array[Byte]] = TypeConverter.instance { span =>

    val bytes = new ByteArrayOutputStream()
    val spanBinaryEncoder = EncoderFactory.get.directBinaryEncoder(bytes, null)
    val wireSpan = implicitly[TypeConverter[Span, avro.Span]].convert(span)
    spanDatumWriter.write(wireSpan, spanBinaryEncoder)
    bytes.toByteArray
  }

  implicit val avroToSpan: TypeConverter[Array[Byte], Span] = TypeConverter.instance { bytes =>
    val spanBinaryDecoder = DecoderFactory.get.binaryDecoder(bytes, 0, bytes.length, null)
    implicitly[TypeConverter[avro.Span, Span]].convert(spanDatumReader.read(null, spanBinaryDecoder))
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
    jsonMapper.addMixInAnnotations(classOf[avro.Note], classOf[IgnoreSpanProperties]);
    jsonMapper.addMixInAnnotations(classOf[avro.NoteValue], classOf[IgnoreSpanProperties]);

    jsonMapper
  }

  implicit val spanToJson: TypeConverter[Span, String] = TypeConverter.instance { span =>
    mapper.writeValueAsString(implicitly[TypeConverter[Span, avro.Span]].convert(span))
  }

  implicit val jsonToSpan: TypeConverter[String, Span] = TypeConverter.instance { str =>
    implicitly[TypeConverter[avro.Span, Span]].convert(mapper.readValue(str, classOf[avro.Span]))
  }
}
