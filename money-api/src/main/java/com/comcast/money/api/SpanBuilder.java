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
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;

public interface SpanBuilder extends io.opentelemetry.api.trace.SpanBuilder {

    /**
     * {@inheritDoc}
     */
    @Override
    SpanBuilder setParent(Context context);

    /**
     * Sets the parent span to the specified span
     */
    SpanBuilder setParent(Span span);

    /**
     * Sets the parent span to the specified span
     */
    SpanBuilder setParent(Optional<Span> span);

    /**
     * {@inheritDoc}
     */
    @Override
    SpanBuilder setNoParent();

    /**
     * Sets whether or not the parent span notes are to be propagated to the created span
     */
    SpanBuilder setSticky(boolean sticky);

    /**
     * {@inheritDoc}
     */
    @Override
    SpanBuilder addLink(SpanContext spanContext);

    /**
     * {@inheritDoc}
     */
    @Override
    SpanBuilder addLink(SpanContext spanContext, Attributes attributes);

    /**
     * {@inheritDoc}
     */
    @Override
    SpanBuilder setAttribute(String key, String value);

    /**
     * {@inheritDoc}
     */
    @Override
    SpanBuilder setAttribute(String key, long value);

    /**
     * {@inheritDoc}
     */
    @Override
    SpanBuilder setAttribute(String key, double value);

    /**
     * {@inheritDoc}
     */
    @Override
    SpanBuilder setAttribute(String key, boolean value);

    /**
     * {@inheritDoc}
     */
    @Override
    <T> SpanBuilder setAttribute(AttributeKey<T> key, T value);

    /**
     * Records the note on the created span
     */
    SpanBuilder record(Note<?> note);

    /**
     * {@inheritDoc}
     */
    @Override
    SpanBuilder setSpanKind(SpanKind spanKind);

    /**
     * {@inheritDoc}
     */
    @Override
    SpanBuilder setStartTimestamp(long startTimestamp, TimeUnit unit);

    /**
     * {@inheritDoc}
     */
    @Override
    SpanBuilder setStartTimestamp(Instant startTimestamp);

    /**
     * {@inheritDoc}
     */
    @Override
    Span startSpan();
}
