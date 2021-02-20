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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

final class PropagatedSpan implements Span, SpanInfo {
    private final SpanId id;
    private final SpanKind kind;
    private final String name;
    private final List<Scope> scopes = new ArrayList<>();

    PropagatedSpan(SpanId id, String name, SpanKind kind) {
        this.id = id;
        this.kind = kind;
        this.name = name;
    }

    @Override
    public Span addEvent(String name, Attributes attributes) {
        return this;
    }

    @Override
    public Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit) {
        return this;
    }

    @Override
    public Span setStatus(StatusCode canonicalCode, String description) {
        return this;
    }

    @Override
    public Span recordException(Throwable exception, Attributes additionalAttributes) {
        return this;
    }

    @Override
    public Span updateName(String name) {
        return this;
    }

    @Override
    public Span record(Note<?> note) {
        return this;
    }

    @Override
    public Scope startTimer(String timerKey) {
        return Scope.noop();
    }

    @Override
    public void stopTimer(String timerKey) { }

    @Override
    public Span attachScope(Scope scope) {
        scopes.add(scope);
        return this;
    }

    @Override
    public SpanInfo info() {
        return this;
    }

    @Override
    public SpanId id() {
        return id;
    }

    @Override
    public long startTimeNanos() {
        return 0L;
    }

    @Override
    public boolean hasEnded() {
        return false;
    }

    @Override
    public long endTimeNanos() {
        return 0L;
    }

    @Override
    public StatusCode status() {
        return StatusCode.UNSET;
    }

    @Override
    public SpanKind kind() {
        return kind;
    }

    @Override
    public String description() {
        return null;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public long durationNanos() {
        return 0L;
    }

    @Override
    public InstrumentationLibrary library() {
        return InstrumentationLibrary.UNKNOWN;
    }

    @Override
    public String appName() {
        return null;
    }

    @Override
    public String host() {
        return null;
    }

    @Override
    public void end() { }

    @Override
    public void end(long timestamp, TimeUnit unit) { }

    @Override
    public SpanContext getSpanContext() {
        return id.toSpanContext();
    }

    @Override
    public boolean isRecording() {
        return false;
    }

    @Override
    public void close() {
        for (Iterator<Scope> iterator = scopes.iterator(); iterator.hasNext(); ) {
            Scope scope = iterator.next();
            iterator.remove();
            scope.close();
        }
    }
}
