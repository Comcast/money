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

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceState;

/**
 * A unique identifier for a Span.
 */
public class SpanId {

    private static final Random rand = new Random();
    private static final String STRING_FORMAT = "SpanId~%s~%s~%s";
    private static final String INVALID_TRACE_ID = "00000000-0000-0000-0000-000000000000";
    private static final SpanId INVALID_SPAN_ID = new SpanId(INVALID_TRACE_ID, 0, 0);
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^([0-9a-f]{8})-([0-9a-f]{4})-([0-9a-f]{4})-([0-9a-f]{4})-([0-9a-f]{12})$", Pattern.CASE_INSENSITIVE);

    private final String traceId;
    private final long parentId;
    private final long selfId;

    /**
     * Creates a new root span ID with a random trace ID and span ID.
     */
    public SpanId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Creates a new root span ID with the specified trace ID and random span ID.
     */
    public SpanId(String traceId) {

        if (traceId == null) {
            this.traceId = UUID.randomUUID().toString();
        } else {
            this.traceId = traceId;
        }
        this.parentId = rand.nextLong();
        this.selfId = this.parentId;
    }

    /**
     * Creates a new child span ID with the specified trace ID and parent span ID and random span ID.
     */
    public SpanId(String traceId, long parentId) {
        this(traceId, parentId, rand.nextLong());
    }

    /**
     * Creates a span ID with the specified trace ID, parent span ID and span ID.
     */
    public SpanId(String traceId, long parentId, long selfId) {

        if (traceId == null) {
            this.traceId = UUID.randomUUID().toString();
        } else {
            this.traceId = traceId;
        }
        this.parentId = parentId;
        this.selfId = selfId;
    }

    /**
     * @return the trace ID
     */
    public String traceId() {
        return traceId;
    }

    public String traceIdAsHex() {
        Matcher matcher = TRACE_ID_PATTERN.matcher(traceId);
        if (matcher.matches()) {
            return matcher.replaceFirst("$1$2$3$4$5")
                    .toLowerCase(Locale.US);
        }
        return traceId;
    }

    /**
     * @return the parent span ID, which will be the same as the span ID in the case of a root span
     */
    public long parentId() {
        return parentId;
    }

    public String parentIdAsHex() {
        return io.opentelemetry.trace.SpanId.fromLong(parentId);
    }

    /**
     * @return the span ID
     */
    public long selfId() {
        return selfId;
    }

    public String selfIdAsHex() {
        return io.opentelemetry.trace.SpanId.fromLong(selfId);
    }

    /**
     * Creates a new child span ID from the current span ID.
     */
    public SpanId newChildId() {
        return new SpanId(traceId, selfId);
    }

    /**
     * @return {@code true} if the span ID is a root span; otherwise, {@code false}
     */
    public boolean isRoot() { return parentId == selfId; }

    /**
     * @return {@code true} if the trace ID and span ID are valid.
     */
    public boolean isValid() {
        return selfId != 0L
                && traceId != null
                && !traceId.isEmpty()
                && !INVALID_TRACE_ID.equals(traceId);
    }

    @Override
    public String toString() {
        return String.format(STRING_FORMAT, traceId, parentId, selfId);
    }

    /**
     * @return the span ID as an OpenTelemetry {@link SpanContext}
     */
    public SpanContext toSpanContext() {
        return toSpanContext(TraceFlags.getSampled(), TraceState.getDefault());
    }

    /**
     * @return the span ID as an OpenTelemetry {@link SpanContext} with the specified trace flags and trace state.
     */
    public SpanContext toSpanContext(byte traceFlags, TraceState traceState) {
        return SpanContext.create(traceIdAsHex(), selfIdAsHex(), traceFlags, traceState);
    }

    /**
     * Creates a span ID from the String format.
     */
    public static SpanId fromString(String spanIdString) {

        String[] parts = spanIdString.split("~");
        if (parts.length < 4) {
            return null;
        }

        String traceId = parts[1].trim();
        long parentId = Long.parseLong(parts[2].trim());
        long selfId = Long.parseLong(parts[3].trim());
        return new SpanId(traceId, parentId, selfId);
    }

    /**
     * Creates a span ID from the OpenTelemetry {@link SpanContext}
     */
    public static SpanId fromSpanContext(SpanContext spanContext) {
        if (spanContext.isValid()) {
            ByteBuffer buffer = ByteBuffer.wrap(spanContext.getTraceIdBytes());
            long traceIdHi = buffer.getLong();
            long traceIdLo = buffer.getLong();
            String traceId = new UUID(traceIdHi, traceIdLo).toString();
            buffer = ByteBuffer.wrap(spanContext.getSpanIdBytes());
            long spanId = buffer.getLong();
            return new SpanId(traceId, spanId, spanId);
        } else {
            return INVALID_SPAN_ID;
        }
    }

    /**
     * Gets an invalid span ID.
     */
    public static SpanId getInvalid() {
        return INVALID_SPAN_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpanId spanId = (SpanId) o;

        if (parentId != spanId.parentId) return false;
        if (selfId != spanId.selfId) return false;
        return traceId.equals(spanId.traceId);

    }

    @Override
    public int hashCode() {
        int result = traceId.hashCode();
        result = 31 * result + (int) (parentId ^ (parentId >>> 32));
        result = 31 * result + (int) (selfId ^ (selfId >>> 32));
        return result;
    }
}
