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

package com.comcast.money.otel.handlers

import java.util
import java.util.Collections

import com.comcast.money.api.{ Event, InstrumentationLibrary, Note, SpanInfo }
import com.comcast.money.core.Money
import io.opentelemetry.api.common.{ Attributes, ReadableAttributes }
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.data.SpanData.Status
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.api.trace.{ SpanContext, SpanId, TraceState, Span => OtelSpan }

import scala.collection.JavaConverters._

private[otel] class MoneyReadableSpanData(info: SpanInfo) extends ReadableSpan with SpanData {
  private val id = info.id
  private lazy val spanContext = id.toSpanContext
  private lazy val libraryInfo = convertLibraryInfo(info.library)
  private lazy val attributes = convertAttributes(info.notes)
  private lazy val events = convertEvents(info.events)

  override def getSpanContext: SpanContext = spanContext
  override def getName: String = info.name
  override def toSpanData: SpanData = this
  override def getInstrumentationLibraryInfo: InstrumentationLibraryInfo = libraryInfo
  override def hasEnded: Boolean = info.endTimeNanos > 0L
  override def getLatencyNanos: Long = info.durationNanos
  override def getTraceId: String = id.traceIdAsHex
  override def getSpanId: String = id.selfIdAsHex
  override def isSampled: Boolean = true
  override def getTraceState: TraceState = TraceState.getDefault
  override def getParentSpanId: String = if (id.isRoot) SpanId.getInvalid else id.parentIdAsHex
  override def getResource: Resource = Resource.getDefault
  override def getKind: OtelSpan.Kind = info.kind
  override def getStartEpochNanos: Long = info.startTimeNanos
  override def getLinks: util.List[SpanData.Link] = Collections.emptyList()
  override def getStatus: SpanData.Status = Status.create(info.status, info.description)
  override def getEndEpochNanos: Long = info.endTimeNanos
  override def getHasRemoteParent: Boolean = false
  override def getHasEnded: Boolean = info.endTimeNanos > 0L
  override def getTotalRecordedEvents: Int = info.events.size
  override def getTotalRecordedLinks: Int = 0
  override def getTotalAttributeCount: Int = info.notes.size
  override def getAttributes: ReadableAttributes = attributes
  override def getEvents: util.List[SpanData.Event] = events

  private def convertLibraryInfo(library: InstrumentationLibrary): InstrumentationLibraryInfo =
    if (library != null) {
      InstrumentationLibraryInfo.create(library.name, library.version)
    } else {
      InstrumentationLibraryInfo.getEmpty
    }

  private def appendNoteToBuilder[T](builder: Attributes.Builder, note: Note[T]): Attributes.Builder =
    builder.put(note.key, note.value)

  private def convertAttributes(notes: util.Map[String, Note[_]]): Attributes =
    notes.values.asScala
      .foldLeft(Attributes.builder) {
        (builder, note) => appendNoteToBuilder(builder, note)
      }
      .build()

  private def convertEvents(events: util.List[Event]): util.List[SpanData.Event] =
    events.asScala
      .map({
        event => MoneyEvent(event).asInstanceOf[SpanData.Event]
      })
      .asJava
}