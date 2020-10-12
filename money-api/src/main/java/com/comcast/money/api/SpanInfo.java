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

import java.util.List;
import java.util.Map;

import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.StatusCanonicalCode;

public interface SpanInfo {

    /**
     * @return a map of all of the notes that were recorded on the span.  Implementers should enforce
     * that the map returned is a copy of the notes
     */
    Map<String, Note<?>> notes();

    /**
     * @return a list of all of the events that were recorded on the span.
     */
    List<Event> events();

    /**
     * @return the time in milliseconds when this span was started
     */
    long startTimeMillis();

    /**
     * @return the time in microseconds when this span was started
     */
    long startTimeMicros();

    /**
     * @return the time in nanoseconds when this span was started
     */
    long startTimeNanos();

    /**
     * @return the time in milliseconds when this span was ended.  Will return
     * null if the span is still open
     */
    long endTimeMillis();

    /**
     * @return the time in microseconds when this span was stopped.
     */
    long endTimeMicros();

    /**
     * @return the time in nanoseconds when this span was stopped.
     */
    long endTimeNanos();

    /**
     * @return the status code set on the span.
     */
    StatusCanonicalCode status();

    /**
     * @return the result of the span.  Will return null if the span was never stopped.
     */
    Boolean success();

    /**
     * @return {@code true} if the span has been started but not yet stopped; otherwise, {@code false}.
     */
    boolean isRecording();

    /**
     * @return the kind of the span, e.g. if it wraps a server or client request.
     */
    Span.Kind kind();

    /**
     * @return the description of the status of the span.
     */
    String description();

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
    long durationMicros();

    /**
     * @return how long since the span was started.  Once it is stopped, the duration should reperesent
     * how long the span was open for.
     */
    long durationNanos();

    /**
     * @return the current application name
     */
    String appName();

    /**
     * @return the host name or ip
     */
    String host();
}
