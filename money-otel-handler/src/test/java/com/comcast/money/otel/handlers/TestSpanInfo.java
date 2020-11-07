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

package com.comcast.money.otel.handlers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

import com.comcast.money.api.InstrumentationLibrary;
import com.comcast.money.api.Note;
import com.comcast.money.api.SpanId;
import com.comcast.money.api.SpanInfo;

public class TestSpanInfo implements SpanInfo {
    private final SpanId spanId;

    public TestSpanInfo(SpanId spanId) {
        this.spanId = spanId;
    }

    @Override
    public Map<String, Note<?>> notes() {
        return Collections.emptyMap();
    }

    @Override
    public List<SpanInfo.Event> events() {
        return Collections.emptyList();
    }

    @Override
    public long startTimeNanos() {
        return 0;
    }

    @Override
    public long endTimeNanos() {
        return 0;
    }

    @Override
    public StatusCode status() {
        return StatusCode.OK;
    }

    @Override
    public Span.Kind kind() {
        return Span.Kind.INTERNAL;
    }

    @Override
    public InstrumentationLibrary library() {
        return new InstrumentationLibrary("test", "0.0.1");
    }

    @Override
    public String description() {
        return "";
    }

    @Override
    public SpanId id() {
        return spanId;
    }

    @Override
    public String name() {
        return "name";
    }

    @Override
    public long durationNanos() {
        return 0;
    }

    @Override
    public String appName() {
        return "appName";
    }

    @Override
    public String host() {
        return "host";
    }
}