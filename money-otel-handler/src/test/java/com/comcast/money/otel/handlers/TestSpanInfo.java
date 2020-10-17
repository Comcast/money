package com.comcast.money.otel.handlers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.StatusCanonicalCode;

import com.comcast.money.api.Event;
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
    public List<Event> events() {
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
    public StatusCanonicalCode status() {
        return StatusCanonicalCode.OK;
    }

    @Override
    public Span.Kind kind() {
        return Span.Kind.INTERNAL;
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