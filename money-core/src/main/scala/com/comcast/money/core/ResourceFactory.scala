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

package com.comcast.money.core

import com.comcast.money.api.{ InstrumentationLibrary, Resource }
import com.typesafe.config.Config
import io.opentelemetry.api.common.{ AttributeKey, Attributes }
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes

import scala.collection.JavaConverters._

private[core] object ResourceFactory {
  def create(
    applicationName: String,
    hostName: String,
    library: InstrumentationLibrary,
    conf: Config): Resource = {

    val attributesBuilder = Attributes.builder()
      .put(ResourceAttributes.SERVICE_NAME, applicationName)
      .put(ResourceAttributes.TELEMETRY_SDK_LANGUAGE, "scala")
      .put(ResourceAttributes.TELEMETRY_SDK_NAME, library.name())
      .put(ResourceAttributes.TELEMETRY_SDK_VERSION, library.version())
      .put(ResourceAttributes.HOST_NAME, hostName)

    val ResourceKey = "resource"
    if (conf.hasPath(ResourceKey)) {
      val resourceConf = conf.getConfig(ResourceKey)
      for (entry <- resourceConf.entrySet().asScala) {
        val key = entry.getKey
        val value = resourceConf.getString(key)
        attributesBuilder.put(AttributeKey.stringKey(key), value)
      }
    }

    CoreResource(
      applicationName = applicationName,
      hostName = hostName,
      library = library,
      attributes = attributesBuilder.build())
  }
}
