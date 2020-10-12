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

import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.Tracer;

/**
 * OpenTelemetry compatible API to be used for tracing
 */
public interface MoneyTracer extends Tracer {

    /**
     * {@inheritDoc}
     */
    @Override
    Span getCurrentSpan();

    /**
     * Enters a scope where the {@link Span} is in the current Context.
     * @see {@link Tracer#withSpan(io.opentelemetry.trace.Span)}
     * @param span The {@link io.opentelemetry.trace.Span} to be set to the current Context.
     * @return an object that defines a scope where the given {@link io.opentelemetry.trace.Span} will be set to the current
     *     Context.
     * @throws NullPointerException if {@code span} is {@code null}.
     */
    Scope withSpan(Span span);

    /**
     * {@inheritDoc}
     */
    @Override
    Span.Builder spanBuilder(String spanName);
}
