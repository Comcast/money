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

import io.grpc.Context;
import io.opentelemetry.common.AttributeKey;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.SpanContext;
import scala.Option;

/**
 * A Span is a container that represents a unit of work.  It could be a long running operation or sequence of
 * statements in process, or a remote system call.
 *
 * A Span is immutable, all changes to the span result in a new Span being created.
 */
public interface Span extends io.opentelemetry.trace.Span, Scope {

    /**
     * Signals the span that it has started
     */
    Scope start();

    /**
     * Signals the span that it has started at the specified timestamp
     * @param startTimeSeconds the seconds since the epoch
     * @param nanoAdjustment the additional nanoseconds from {@code startTimeSeconds}
     */
    Scope start(long startTimeSeconds, int nanoAdjustment);

    /**
     * Stops the span asserts a successful result
     */
    void stop();

    /**
     * Ends a span, moving it to a Stopped state
     * @param result The result of the span (success or failure)
     */
    void stop(Boolean result);

    /**
     * Records a given note onto the span.  If the note was already present, it will be overwritten
     * @param note The note to be recorded
     */
    void record(Note<?> note);

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

    /**
     * A builder used to construct {@link Span} instances.
     */
    interface Builder extends io.opentelemetry.trace.Span.Builder {
        /**
         * {@inheritDoc}
         */
        @Override
        Builder setParent(Context context);

        /**
         * Sets the parent span to the specified span
         */
        Builder setParent(Span span);

        /**
         * Sets the parent span to the specified span
         */
        Builder setParent(Option<Span> span);

        /**
         * {@inheritDoc}
         */
        @Override
        Builder setNoParent();

        /**
         * Sets whether or not the parent span notes are to be propagated to the created span
         */
        Builder setSticky(boolean sticky);

        /**
         * {@inheritDoc}
         */
        @Override
        Builder addLink(SpanContext spanContext);

        /**
         * {@inheritDoc}
         */
        @Override
        Builder addLink(SpanContext spanContext, Attributes attributes);

        /**
         * {@inheritDoc}
         */
        @Override
        Builder setAttribute(String key, String value);

        /**
         * {@inheritDoc}
         */
        @Override
        Builder setAttribute(String key, long value);

        /**
         * {@inheritDoc}
         */
        @Override
        Builder setAttribute(String key, double value);

        /**
         * {@inheritDoc}
         */
        @Override
        Builder setAttribute(String key, boolean value);

        /**
         * {@inheritDoc}
         */
        @Override
        <T> Builder setAttribute(AttributeKey<T> key, T value);

        /**
         * Records the note on the created span
         */
        Builder record(Note<?> note);

        /**
         * {@inheritDoc}
         */
        @Override
        Builder setSpanKind(Kind spanKind);

        /**
         * {@inheritDoc}
         */
        @Override
        Builder setStartTimestamp(long startTimestampNanos);

        /**
         * Creates the new span without starting it.
         */
        Span build();

        /**
         * {@inheritDoc}
         */
        @Override
        Span startSpan();
    }
}
