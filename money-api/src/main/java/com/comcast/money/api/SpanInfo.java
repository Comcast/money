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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;

public interface SpanInfo {

    /**
     * @return a map of all of the notes that were recorded on the span.  Implementers should enforce
     * that the map returned is a copy of the notes
     */
    Map<String, Note<?>> notes();

    /**
     * @return a list of all of the events that were recorded on the span.
     */
    default List<SpanEvent> events() {
        return Collections.emptyList();
    }

    /**
     * @return a list of the spans linked to the span
     */
    default List<SpanLink> links() {
        return Collections.emptyList();
    }

    /**
     * @return the time in milliseconds when this span was started
     */
    default long startTimeMillis() {
        return TimeUnit.NANOSECONDS.toMillis(startTimeNanos());
    }

    /**
     * @return the time in microseconds when this span was started
     */
    default long startTimeMicros() {
        return TimeUnit.NANOSECONDS.toMicros(startTimeNanos());
    }

    /**
     * @return the time in nanoseconds when this span was started
     */
    long startTimeNanos();

    /**
     * @return the time in milliseconds when this span was ended.  Will return
     * null if the span is still open
     */
    default long endTimeMillis() {
        return TimeUnit.NANOSECONDS.toMillis(endTimeNanos());
    }

    /**
     * @return the time in microseconds when this span was stopped.
     */
    default long endTimeMicros() {
        return TimeUnit.NANOSECONDS.toMicros(endTimeNanos());
    }

    /**
     * @return the time in nanoseconds when this span was stopped.
     */
    long endTimeNanos();

    /**
     * @return the status code set on the span.
     */
    StatusCode status();

    /**
     * @return the result of the span.  Will return null if the span was never stopped.
     */
    default Boolean success() {
        StatusCode status = status();
        if (status != null && endTimeNanos() > 0L) {
            switch (status) {
                case OK:
                    return true;
                case ERROR:
                    return false;
            }
        }
        return null;
    }

    /**
     * @return {@code true} if the span has been started but not yet stopped; otherwise, {@code false}.
     */
    default boolean isRecording() {
        return startTimeNanos() > 0L && endTimeNanos() <= 0L;
    }

    /**
     * @return the kind of the span, e.g. if it wraps a server or client request.
     */
    SpanKind kind();

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
    default long durationMicros() {
        return TimeUnit.NANOSECONDS.toMicros(durationNanos());
    }

    /**
     * @return how long since the span was started.  Once it is stopped, the duration should reperesent
     * how long the span was open for.
     */
    long durationNanos();

    /**
     * @return the instrumentation library that initiated the span
     */
    InstrumentationLibrary library();

    /**
     * @return the current application name
     */
    String appName();

    /**
     * @return the host name or ip
     */
    String host();

}
