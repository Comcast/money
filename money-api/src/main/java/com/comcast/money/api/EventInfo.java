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
 * An event that was recorded on a {@link Span}.
 */
public interface EventInfo {
    /**
     * @return the name of the event
     */
    String name();

    /**
     * @return the attributes recorded on the event
     */
    Attributes attributes();

    /**
     * @return the timestamp of when the event occurred in nanoseconds since the epoch
     */
    long timestamp();

    /**
     * @return an exception if one was recorded with the event; otherwise {@code null}
     */
    Throwable exception();
}
