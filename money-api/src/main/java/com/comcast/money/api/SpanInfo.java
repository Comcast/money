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

package com.comcast.money.api;

import java.util.Map;

public interface SpanInfo {

    /**
     * @return a map of all of the notes that were recorded on the span.  Implementers should enforce
     * that the map returned is a copy of the notes
     */
    Map<String, Note<?>> notes();

    /**
     * @return the time in milliseconds when this span was started
     */
    Long startTimeMillis();

    /**
     * @return the time in microseconds when this span was started
     */
    Long startTimeMicros();

    /**
     * @return the time in milliseconds when this span was ended.  Will return
     * null if the span is still open
     */
    Long endTimeMillis();

    /**
     * @return the time in microseconds when this span was stopped.
     */
    Long endTimeMicros();

    /**
     * @return the result of the span.  Will return null if the span was never stopped.
     */
    Boolean success();

    /**
     * @return the SpanId of the span.
     */
    SpanId id();

    /**
     * @return the name of the span
     */
    String name();

    /**
     * @return how long since the span was started.  Once it is stopped, the duration should reperesent
     * how long the span was open for.
     */
    Long durationMicros();

    /**
     * @return the current application name
     */
    String appName();

    /**
     * @return the host name or ip
     */
    String host();
}
