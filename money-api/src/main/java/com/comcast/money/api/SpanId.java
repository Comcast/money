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

/**
 * A unique identifier for a Span.
 */
public final class SpanId {

    private static final Random rand = new Random();
    private static final String INVALID_TRACE_ID = "00000000-0000-0000-0000-000000000000";
    private static final SpanId INVALID_SPAN_ID = new SpanId(INVALID_TRACE_ID, 0L, 0L, false, (byte) 0, TraceState.getDefault());
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^([0-9a-f]{8})-?([0-9a-f]{4})-([0-9a-f]{4})-?([0-9a-f]{4})-?([0-9a-f]{12})$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRACE_ID_HEX_PATTERN = Pattern.compile("^(?:[0-9a-f]{16}){1,2}$", Pattern.CASE_INSENSITIVE);
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
        String traceId = UUID.randomUUID().toString();
        long selfId = randomNonZeroLong();
        byte flags = sampled ? TraceFlags.getSampled() : TraceFlags.getDefault();
        return new SpanId(traceId, selfId, selfId, false, flags, TraceState.getDefault());
    }

    /**
     * Creates a new child span ID inheriting state from the parent span ID
     */
    public static SpanId createChild(SpanId parentSpanId) {

        if (parentSpanId != null && parentSpanId.isValid()) {
            return new SpanId(parentSpanId.traceId, parentSpanId.selfId, randomNonZeroLong(), false, parentSpanId.flags, parentSpanId.state);
        } else {
            return createNew();
        }
    }

    /**
     * @return a new remote span ID
     */
    public static SpanId createRemote(String traceId, long parentId, long selfId, byte flags, TraceState state) {

        Objects.requireNonNull(traceId);
        Matcher matcher = TRACE_ID_PATTERN.matcher(traceId);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("traceId is not in the required format: '" + traceId + "'");
        }
        if (parentId == 0) {
            parentId = selfId;
        }
        return new SpanId(traceId, parentId, selfId, true, flags, state);
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

    /**
     * Generates a random non-zero 64-bit ID that can be used as span ID
     */
    public static long randomNonZeroLong() {
        long id;
        do {
            id = rand.nextLong();
        } while (id == 0L);
        return id;
    }

    /**
     * Generates a random trace ID.
     */
    public static String randomTraceId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Parses a 64-bit or 128-bit hexadecimal string into a trace ID.
     */
    public static String parseTraceIdFromHex(String traceIdAsHex) {

        Objects.requireNonNull(traceIdAsHex);
        Matcher matcher = TRACE_ID_HEX_PATTERN.matcher(traceIdAsHex);
        if (matcher.matches()) {
            if (traceIdAsHex.length() == 16) {
                // 64-bit trace ID, pad it to 128-bit
                traceIdAsHex = "0000000000000000" + traceIdAsHex;
            }
            return new StringBuilder(36)
                    .append(traceIdAsHex.subSequence(0, 8))
                    .append('-')
                    .append(traceIdAsHex.subSequence(8, 12))
                    .append('-')
                    .append(traceIdAsHex.subSequence(12, 16))
                    .append('-')
                    .append(traceIdAsHex.subSequence(16, 20))
                    .append('-')
                    .append(traceIdAsHex.subSequence(20, 32))
                    .toString()
                    .toLowerCase(Locale.US);
        }
        throw new IllegalArgumentException("traceId is not in a supported hexadecimal format: '" + traceIdAsHex + "'");
    }

    /**
     * Parses a 64-bit hexadecimal string into a numeric ID.
     */
    public static long parseIdFromHex(String idAsHex) {

        Objects.requireNonNull(idAsHex);
        if (idAsHex.length() == 16) {
            try {
                return Long.parseUnsignedLong(idAsHex, 16);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Id is not in a supported hexadecimal format: '" + idAsHex + "'", exception);
            }
        }
        throw new IllegalArgumentException("Id is not in a supported hexadecimal format: '" + idAsHex + "'");
    }

    // for testing purposes
    SpanId(String traceId, long parentId, long selfId) {
        this(traceId, parentId, selfId, false, (byte) 0, TraceState.getDefault());
    }

    private SpanId(String traceId, long parentId, long selfId, boolean remote, byte flags, TraceState traceState) {
        this.traceId = traceId.toLowerCase(Locale.US);
        this.parentId = parentId;
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
        return traceId.replace("-", "");
    }

    /**
     * @return the parent span ID, which will be the same as the span ID in the case of a root span
     */
    public long parentId() {
        return parentId != 0 ? parentId : selfId;
    }

    /**
     * @return the parent span ID, formatted as 16 lowercase hexadecimal characters
     */
    public String parentIdAsHex() {
        return io.opentelemetry.trace.SpanId.fromLong(parentId);
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
        return io.opentelemetry.trace.SpanId.fromLong(selfId);
    }

    /**
     * @return {@code true} if the span ID is a root span; otherwise, {@code false}
     */
    public boolean isRoot() {
        return parentId == 0 || parentId == selfId;
    }

    /**
     * @return {@code true} if the trace ID and span ID are valid.
     */
    public boolean isValid() {
        return selfId != 0L && !INVALID_TRACE_ID.equals(traceId);
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
        return toSpanContext(TraceFlags.getDefault(), TraceState.getDefault());
    }

    /**
     * @return the span ID as an OpenTelemetry {@link SpanContext} with the specified trace flags and trace state.
     */
    public SpanContext toSpanContext(byte traceFlags, TraceState traceState) {
        return SpanContext.create(traceIdAsHex(), selfIdAsHex(), traceFlags, traceState);
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
