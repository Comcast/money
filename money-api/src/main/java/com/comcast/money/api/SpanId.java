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

package com.comcast.money.api;

import java.util.Random;
import java.util.UUID;

/**
 * A unique identifier for a Span.
 */
public class SpanId {

    private static final Random rand = new Random();
    private static final String STRING_FORMAT = "SpanId~%s~%s~%s";

    private final String traceId;
    private final long parentId;
    private final long selfId;

    public SpanId() {
        this(UUID.randomUUID().toString());
    }

    public SpanId(String traceId) {

        if (traceId == null) {
            this.traceId = UUID.randomUUID().toString();
        } else {
            this.traceId = traceId;
        }
        this.parentId = rand.nextLong();
        this.selfId = this.parentId;
    }

    public SpanId(String traceId, long parentId) {
        this(traceId, parentId, rand.nextLong());
    }

    public SpanId(String traceId, long parentId, long selfId) {

        if (traceId == null) {
            this.traceId = UUID.randomUUID().toString();
        } else {
            this.traceId = traceId;
        }
        this.parentId = parentId;
        this.selfId = selfId;
    }

    public String traceId() {
        return traceId;
    }

    public long parentId() {
        return parentId;
    }

    public long selfId() {
        return selfId;
    }

    public SpanId newChildId() {
        return new SpanId(traceId, selfId);
    }

    public boolean isRoot() {return  parentId == selfId;}

    @Override
    public String toString() {
        return String.format(STRING_FORMAT, traceId, parentId, selfId);
    }

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
