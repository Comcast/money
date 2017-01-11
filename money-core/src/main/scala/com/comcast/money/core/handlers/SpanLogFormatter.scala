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

package com.comcast.money.core.handlers

import com.comcast.money.api.SpanInfo
import com.typesafe.config.Config

object SpanLogFormatter {
  def apply(conf: Config) = {
    implicit val c: Config = conf
    new SpanLogFormatter(
      spanStart = configValue("formatting.span-start", "Span: "),
      nullValue = configValue("formatting.null-value", "NULL"),
      logTemplate = configValue("formatting.log-template", "[ %s=%s ]"),
      spanDurationMsEnabled = configEnabled("formatting.span-duration-ms-enabled"),
      spanIdKey = configValue("formatting.keys.span-id", "span-id"),
      traceIdKey = configValue("formatting.keys.trace-id", "trace-id"),
      parentIdKey = configValue("formatting.keys.parent-id", "parent-id"),
      spanNameKey = configValue("formatting.keys.span-name", "span-name"),
      appNameKey = configValue("formatting.keys.app-name", "app-name"),
      startTimeKey = configValue("formatting.keys.start-time", "start-time"),
      spanDurationKey = configValue("formatting.keys.span-duration", "span-duration"),
      spanDurationMsKey = configValue("formatting.keys.span-duration-ms", "span-duration-ms"),
      spanSuccessKey = configValue("formatting.keys.span-success", "span-success")
    )
  }

  private def configValue(key: String, defaultValue: String)(implicit conf: Config) =
    if (conf.hasPath(key))
      conf.getString(key)
    else
      defaultValue

  private def configEnabled(key: String)(implicit conf: Config): Boolean =
    if (conf.hasPath(key))
      conf.getString(key).toBoolean
    else
      false
}

class SpanLogFormatter(
    val spanStart: String,
    val nullValue: String,
    val logTemplate: String,
    val spanDurationMsEnabled: Boolean,
    val spanIdKey: String,
    val traceIdKey: String,
    val parentIdKey: String,
    val spanNameKey: String,
    val appNameKey: String,
    val startTimeKey: String,
    val spanDurationKey: String,
    val spanDurationMsKey: String,
    val spanSuccessKey: String
) {

  def buildMessage(spanInfo: SpanInfo): String = {
    implicit val builder = new StringBuilder()
    builder.append(spanStart)
    append(spanIdKey, spanInfo.id.selfId)
    append(traceIdKey, spanInfo.id.traceId)
    append(parentIdKey, spanInfo.id.parentId)
    append(spanNameKey, spanInfo.name)
    append(appNameKey, spanInfo.appName)
    append(startTimeKey, spanInfo.startTimeMillis)
    append(spanDurationKey, spanInfo.durationMicros)

    if (spanDurationMsEnabled)
      append(spanDurationMsKey, spanInfo.durationMicros / 1000)

    append(spanSuccessKey, spanInfo.success)

    val tags = spanInfo.tags.values().iterator()

    while (tags.hasNext) {
      val note = tags.next
      append(note.name, valueOrNull(note.value))
    }

    builder.toString()
  }

  private def append[T](key: String, value: T)(implicit builder: StringBuilder): StringBuilder =
    builder.append(logTemplate.format(key, value))

  private def valueOrNull[T](value: T) =
    if (value == null)
      nullValue
    else
      value
}
