/*
 * Copyright 2012-2015 Comcast Cable Communications Management, LLC
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

package com.comcast.money.core;

import java.util.Random;
import java.util.UUID;

/**
 * A unique identifier for a Span
 */
public class SpanId {

    private static final Random rand = new Random();
    private static final String stringFormat = "SpanId~%s~%s~%s";

    private final String traceId;
    private final Long parentId;
    private final Long selfId;

    public SpanId() {
        this(UUID.randomUUID().toString());
    }

    public SpanId(String traceId) {
        this(traceId, rand.nextLong());
    }

    public SpanId(String traceId, Long parentId) {
        this(traceId, parentId, rand.nextLong());
    }

    public SpanId(String traceId, Long parentId, Long selfId) {
        this.traceId = traceId;
        this.parentId = parentId;
        this.selfId = selfId;
    }

    public String getTraceId() {
        return traceId;
    }

    public Long getParentId() {
        return parentId;
    }

    public Long getSelfId() {
        return selfId;
    }

    public SpanId newChild() {
        return new SpanId(traceId, selfId);
    }

    @Override
    public String toString() {
        return String.format(stringFormat, traceId, parentId, selfId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpanId spanId = (SpanId) o;

        if (!getTraceId().equals(spanId.getTraceId())) return false;
        if (!getParentId().equals(spanId.getParentId())) return false;
        return getSelfId().equals(spanId.getSelfId());

    }

    @Override
    public int hashCode() {
        int result = getTraceId().hashCode();
        result = 31 * result + getParentId().hashCode();
        result = 31 * result + getSelfId().hashCode();
        return result;
    }
}
