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
import com.comcast.money.api.{ InstrumentationLibrary, Note, SpanEvent, SpanId, SpanInfo, SpanLink }
import io.opentelemetry.api.common.{ Attributes, AttributesBuilder }
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.data.{ EventData, LinkData, SpanData, StatusData }
import io.opentelemetry.api.trace.{ SpanContext, SpanKind }
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes.{ HOST_NAME, SERVICE_NAME, TELEMETRY_SDK_LANGUAGE, TELEMETRY_SDK_NAME, TELEMETRY_SDK_VERSION }

import scala.collection.JavaConverters._

private[otel] class MoneyReadableSpanData(info: SpanInfo) extends ReadableSpan with SpanData {
  private val id = info.id
  private lazy val spanContext = id.toSpanContext
  private lazy val parentSpanContext = convertParentSpanContext(info.id)
  private lazy val libraryInfo = convertLibraryInfo(info.library)
  private lazy val attributes = convertAttributes(info.notes)
  private lazy val events = convertEvents(info.events)
  private lazy val links = convertLinks(info.links)
  private lazy val resource = createResource(info)

  override def getSpanContext: SpanContext = spanContext
  override def getParentSpanContext: SpanContext = parentSpanContext
  override def getName: String = info.name
  override def toSpanData: SpanData = this
  override def getInstrumentationLibraryInfo: InstrumentationLibraryInfo = libraryInfo
  override def hasEnded: Boolean = info.endTimeNanos > 0L
  override def getLatencyNanos: Long = info.durationNanos
  override def getTraceId: String = id.traceIdAsHex
  override def getSpanId: String = id.selfIdAsHex
  override def getResource: Resource = resource
  override def getKind: SpanKind = info.kind
  override def getStartEpochNanos: Long = info.startTimeNanos
  override def getLinks: util.List[LinkData] = links
  override def getStatus: StatusData = StatusData.create(info.status, info.description)
  override def getEndEpochNanos: Long = info.endTimeNanos
  override def getTotalRecordedEvents: Int = info.events.size
  override def getTotalRecordedLinks: Int = 0
  override def getTotalAttributeCount: Int = info.notes.size
  override def getAttributes: Attributes = attributes
  override def getEvents: util.List[EventData] = events

  private def convertParentSpanContext(id: SpanId): SpanContext =
    if (id.isRoot) {
      SpanContext.getInvalid
    } else {
      id.parentSpanId().toSpanContext
    }

  private def convertLibraryInfo(library: InstrumentationLibrary): InstrumentationLibraryInfo =
    if (library != null) {
      InstrumentationLibraryInfo.create(library.name, library.version)
    } else {
      InstrumentationLibraryInfo.empty
    }

  private def createResource(info: SpanInfo): Resource =
    Resource.create(Attributes.builder()
      .put(TELEMETRY_SDK_NAME, "money")
      .put(TELEMETRY_SDK_LANGUAGE, "java")
      .put(TELEMETRY_SDK_VERSION, "0.18.0")
      .put(SERVICE_NAME, info.appName)
      .put(HOST_NAME, info.host)
      .build())

  private def appendNoteToBuilder[T](builder: AttributesBuilder, note: Note[T]): AttributesBuilder =
    builder.put(note.key, note.value)

  private def convertAttributes(notes: util.Map[String, Note[_]]): Attributes =
    notes.values.asScala
      .foldLeft(Attributes.builder) {
        (builder, note) => appendNoteToBuilder(builder, note)
      }
      .build()

  private def convertEvents(events: util.List[SpanEvent]): util.List[EventData] =
    events.asScala
      .map({
        event => MoneyEvent(event).asInstanceOf[EventData]
      })
      .asJava

  private def convertLinks(links: util.List[SpanLink]): util.List[LinkData] =
    links.asScala
      .map({
        link => MoneyLink(link).asInstanceOf[LinkData]
      })
      .asJava
}