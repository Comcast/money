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

package com.comcast.money.api;

import io.opentelemetry.api.common.Attributes;

/**
 * Represents a resource, which capture identifying information about the entities
 * for which traces are reported.
 */
public interface Resource {
    /**
     * @return the current application name
     */
    String applicationName();

    /**
     * @return the host name or ip
     */
    String hostName();

    /**
     * @return the tracing library
     */
    InstrumentationLibrary library();

    /**
     * @return a map of attributes that describe the resource
     */
    Attributes attributes();
}
