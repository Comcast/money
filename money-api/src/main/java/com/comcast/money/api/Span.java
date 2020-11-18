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

    /**
     * Stops the span asserts a successful result
     */
    void stop();

    /**
     * Ends a span, moving it to a Stopped state
     * @param result The result of the span (success or failure)
     */
    void stop(Boolean result);

    @Override
    Span setAttribute(String key, String value);

    @Override
    Span setAttribute(String key, long value);

    @Override
    Span setAttribute(String key, double value);

    @Override
    Span setAttribute(String key, boolean value);

    @Override
    <T> Span setAttribute(AttributeKey<T> key, T value);

    @Override
    Span setAttribute(AttributeKey<Long> key, int value);

    @Override
    Span addEvent(String name);

    @Override
    Span addEvent(String name, long timestamp, TimeUnit unit);

    @Override
    Span addEvent(String name, Instant timestamp);

    @Override
    Span addEvent(String name, Attributes attributes);

    @Override
    Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit);

    @Override
    Span addEvent(String name, Attributes attributes, Instant timestamp);

    @Override
    Span setStatus(StatusCode canonicalCode);

    @Override
    Span setStatus(StatusCode canonicalCode, String description);

    @Override
    Span recordException(Throwable exception);

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
     * @return The current state of the Span
     */
    SpanInfo info();
}
