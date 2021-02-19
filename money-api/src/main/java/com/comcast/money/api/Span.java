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

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

/**
 * A Span is a container that represents a unit of work.  It could be a long running operation or sequence of
 * statements in process, or a remote system call.
 *
 * A Span is immutable, all changes to the span result in a new Span being created.
 */
public interface Span extends io.opentelemetry.api.trace.Span, Scope {

    @Override
    default Span setAttribute(String key, String value) {
        return record(Note.of(key, value));
    }

    @Override
    default Span setAttribute(String key, long value) {
        return record(Note.of(key, value));
    }

    @Override
    default Span setAttribute(String key, double value) {
        return record(Note.of(key, value));
    }

    @Override
    default Span setAttribute(String key, boolean value) {
        return record(Note.of(key, value));
    }

    @Override
    default <T> Span setAttribute(AttributeKey<T> key, T value) {
        return record(Note.of(key, value));
    }

    @Override
    default Span setAttribute(AttributeKey<Long> key, int value) {
        return setAttribute(key, (long) value);
    }

    @Override
    default Span addEvent(String name) {
        return addEvent(name, Attributes.empty());
    }

    @Override
    default Span addEvent(String name, long timestamp, TimeUnit unit) {
        return addEvent(name, Attributes.empty(), timestamp, unit);
    }

    @Override
    default Span addEvent(String name, Instant timestamp) {
        return addEvent(name, Attributes.empty(), timestamp);
    }

    @Override
    Span addEvent(String name, Attributes attributes);

    @Override
    Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit);

    @Override
    default Span addEvent(String name, Attributes attributes, Instant timestamp) {
        if (timestamp != null) {
            return addEvent(name, attributes, TimeUnit.SECONDS.toNanos(timestamp.getEpochSecond()) + timestamp.getNano(), TimeUnit.NANOSECONDS);
        } else {
            return addEvent(name, attributes);
        }
    }

    @Override
    default Span setStatus(StatusCode canonicalCode) {
        return setStatus(canonicalCode, null);
    }

    @Override
    Span setStatus(StatusCode canonicalCode, String description);

    @Override
    default Span recordException(Throwable exception) {
        return recordException(exception, Attributes.empty());
    }

    @Override
    Span recordException(Throwable exception, Attributes additionalAttributes);

    @Override
    Span updateName(String name);

    /**
     * Records a given note onto the span.  If the note was already present, it will be overwritten
     * @param note The note to be recorded
     */
    Span record(Note<?> note);

    /**
     * Starts a new timer on the span
     * @param timerKey The name of the timer to start
     */
    Scope startTimer(String timerKey);

    /**
     * Stops an existing timer on the span
     * @param timerKey The name of the timer
     */
    void stopTimer(String timerKey);

    /**
     * Attaches a {@link Scope} to the span which will be closed when the span is stopped
     */
    Span attachScope(Scope scope);

    /**
     * Ends the span
     * @param result The result of the span (success or failure)
     */
    default void end(boolean result) {
        setStatus(result ? StatusCode.OK : StatusCode.ERROR).end();
    }

    /**
     * @return The current state of the Span
     */
    SpanInfo info();

    /**
     * @return The {@link SpanId} of the Span
     */
    default SpanId id() {
        return info().id();
    }
}
