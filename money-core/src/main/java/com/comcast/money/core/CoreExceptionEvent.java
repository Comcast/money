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

package com.comcast.money.core;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import com.comcast.money.api.EventInfo;

final class CoreExceptionEvent implements EventInfo {
    private final Throwable exception;
    private final long timestampNanos;
    private final Attributes additionalAttributes;
    private final Object lock = new Object();
    private transient Attributes attributes;

    public CoreExceptionEvent(Throwable exception, long timestampNanos) {
        this(exception, timestampNanos, Attributes.empty());
    }

    public CoreExceptionEvent(Throwable exception, long timestampNanos, Attributes attributes) {
        this.timestampNanos = timestampNanos;
        this.exception = exception;
        this.additionalAttributes = attributes;
    }

    @Override
    public String name() {
        return SemanticAttributes.EXCEPTION_EVENT_NAME;
    }

    @Override
    public Attributes attributes() {
        if (attributes != null) {
            return attributes;
        }
        synchronized (lock) {
            if (attributes != null) {
                return attributes;
            }
            AttributesBuilder builder = additionalAttributes.toBuilder();
            builder.put(SemanticAttributes.EXCEPTION_TYPE, exception.getClass().getCanonicalName());
            String message = exception.getMessage();
            if (message != null && !message.isEmpty()) {
                builder.put(SemanticAttributes.EXCEPTION_MESSAGE, message);
            }
            StringWriter writer = new StringWriter();
            exception.printStackTrace(new PrintWriter(writer));
            builder.put(SemanticAttributes.EXCEPTION_STACKTRACE, writer.toString());
            attributes = builder.build();
            return attributes;
        }
    }

    @Override
    public long timestamp() {
        return timestampNanos;
    }

    @Override
    public Throwable exception() {
        return exception;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CoreExceptionEvent that = (CoreExceptionEvent) o;

        if (timestampNanos != that.timestampNanos) return false;
        if (!exception.equals(that.exception)) return false;
        return additionalAttributes.equals(that.additionalAttributes);
    }

    @Override
    public int hashCode() {
        int result = exception.hashCode();
        result = 31 * result + (int) (timestampNanos ^ (timestampNanos >>> 32));
        result = 31 * result + additionalAttributes.hashCode();
        return result;
    }
}
