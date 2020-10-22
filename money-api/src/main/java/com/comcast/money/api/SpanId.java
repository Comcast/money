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
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceState;

import static com.comcast.money.api.IdGenerator.INVALID_ID;
import static com.comcast.money.api.IdGenerator.INVALID_TRACE_ID;

/**
 * A unique identifier for a Span.
 */
public final class SpanId {

    private static final SpanId INVALID_SPAN_ID = new SpanId(INVALID_TRACE_ID, INVALID_ID, INVALID_ID, false, TraceFlags.getDefault(), TraceState.getDefault());
    private static final byte SAMPLED = TraceFlags.getSampled();

    private final String traceId;
    private final long parentId;
    private final long selfId;
    private final boolean remote;
    private final byte flags;
    private final TraceState state;

    /**
     * Creates a new root span ID
     */
    public static SpanId createNew() {
        return createNew(true);
    }

    /**
     * Creates a new root span ID
     */
    public static SpanId createNew(boolean sampled) {
        String traceId = IdGenerator.generateRandomTraceId();
        long selfId = IdGenerator.generateRandomId();
        byte flags = sampled ? TraceFlags.getSampled() : TraceFlags.getDefault();
        return new SpanId(traceId, selfId, selfId, false, flags, TraceState.getDefault());
    }

    /**
     * Creates a new child span ID inheriting state from the parent span ID
     */
    public static SpanId createChild(SpanId parentSpanId) {

        if (parentSpanId != null && parentSpanId.isValid()) {
            long selfId = IdGenerator.generateRandomId();
            return new SpanId(parentSpanId.traceId, parentSpanId.selfId, selfId, false, parentSpanId.flags, parentSpanId.state);
        } else {
            return createNew();
        }
    }

    /**
     * @return a new remote span ID
     */
    public static SpanId createRemote(String traceId, long parentId, long selfId, byte flags, TraceState state) {

        Objects.requireNonNull(traceId);
        if (!IdGenerator.isValidTraceId(traceId)) {
            throw new IllegalArgumentException("traceId is not in the required format: '" + traceId + "'");
        }
        return new SpanId(traceId, parentId, selfId, true, flags, state);
    }

    /**
     * @return a new span from the specified parameters, should only be used for testing
     */
    public static SpanId createFrom(UUID traceId, long parentId, long selfId) {
        return createFrom(traceId, parentId, selfId, false, TraceFlags.getSampled(), TraceState.getDefault());
    }

    /**
     * @return a new span from the specified parameters, should only be used for testing
     */
    public static SpanId createFrom(UUID traceId, long parentId, long selfId, boolean remote, byte flags, TraceState state) {
        return new SpanId(traceId.toString(), parentId, selfId, remote, flags, state);
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
            return new SpanId(traceId, spanId, spanId,
                    spanContext.isRemote(),
                    spanContext.getTraceFlags(),
                    spanContext.getTraceState());
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

    // for testing purposes
    SpanId(String traceId, long parentId, long selfId) {
        this(traceId, parentId, selfId, false, (byte) 0, TraceState.getDefault());
    }

    SpanId(String traceId, long parentId, long selfId, boolean remote, byte flags, TraceState traceState) {
        this.traceId = traceId.toLowerCase(Locale.US);
        this.parentId = parentId != 0 ? parentId : selfId;
        this.selfId = selfId;
        this.remote = remote;
        this.flags = flags;
        this.state = traceState != null ? traceState : TraceState.getDefault();
    }

    /**
     * @return the trace ID
     */
    public String traceId() {
        return traceId;
    }

    /**
     * @return the trace ID formatted as 32 lowercase hexadecimal characters
     */
    public String traceIdAsHex() {
        return IdGenerator.convertTraceIdToHex(traceId);
    }

    /**
     * @return the parent span ID, which will be the same as the span ID in the case of a root span
     */
    public long parentId() {
        return parentId;
    }

    /**
     * @return the parent span ID, formatted as 16 lowercase hexadecimal characters
     */
    public String parentIdAsHex() {
        return IdGenerator.convertIdToHex(parentId);
    }

    /**
     * @return the span ID
     */
    public long selfId() {
        return selfId;
    }

    /**
     * @return the span ID formatted as 16 lowercase hexadecimal characters
     */
    public String selfIdAsHex() {
        return IdGenerator.convertIdToHex(selfId);
    }

    /**
     * @return the bitmask of flags for the span ID
     */
    public byte traceFlags() {
        return flags;
    }

    /**
     * @return the state attached to the span in name/value pairs
     */
    public TraceState traceState() {
        return state;
    }

    /**
     * @return {@code true} if the span ID is a root span; otherwise, {@code false}
     */
    public boolean isRoot() {
        return parentId == selfId;
    }

    /**
     * @return {@code true} if the trace ID and span ID are valid.
     */
    public boolean isValid() {
        return selfId != INVALID_ID && !INVALID_TRACE_ID.equals(traceId);
    }

    /**
     * @return {@code true} if the span ID represents a remote span
     */
    public boolean isRemote() {
        return remote;
    }

    /**
     * @return {@code true} if the span ID is sampled
     */
    public boolean isSampled() {
        return (flags & SAMPLED) == SAMPLED;
    }

    /**
     * Creates a new child span ID from the current span ID
     */
    public SpanId createChild() {
        return createChild(this);
    }

    /**
     * @return the span ID as an OpenTelemetry {@link SpanContext}
     */
    public SpanContext toSpanContext() {
        return toSpanContext(flags, state);
    }

    /**
     * @return the span ID as an OpenTelemetry {@link SpanContext} with the specified trace flags and trace state.
     */
    public SpanContext toSpanContext(byte traceFlags, TraceState traceState) {
        if (remote) {
            return SpanContext.createFromRemoteParent(traceIdAsHex(), selfIdAsHex(), traceFlags, traceState);
        } else {
            return SpanContext.create(traceIdAsHex(), selfIdAsHex(), traceFlags, traceState);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpanId spanId = (SpanId) o;

        if (parentId != spanId.parentId) return false;
        if (selfId != spanId.selfId) return false;
        if (remote != spanId.remote) return false;
        if (flags != spanId.flags) return false;
        if (!traceId.equals(spanId.traceId)) return false;
        return state.equals(spanId.state);
    }

    @Override
    public int hashCode() {
        int result = traceId.hashCode();
        result = 31 * result + (int) (parentId ^ (parentId >>> 32));
        result = 31 * result + (int) (selfId ^ (selfId >>> 32));
        result = 31 * result + (remote ? 1 : 0);
        result = 31 * result + (int) flags;
        result = 31 * result + state.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SpanId{" +
                "traceId='" + traceId + '\'' +
                ", parentId=" + parentId +
                ", selfId=" + selfId +
                ", remote=" + remote +
                ", flags=" + flags +
                ", state=" + state +
                '}';
    }
}
