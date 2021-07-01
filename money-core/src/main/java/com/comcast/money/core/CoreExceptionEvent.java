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
import lombok.EqualsAndHashCode;
import lombok.Getter;

import com.comcast.money.api.EventInfo;

@EqualsAndHashCode(exclude = {"additionalAttributes", "lock"})
final class CoreExceptionEvent implements EventInfo {
    @Getter private final Throwable exception;
    @Getter private final long timestampNanos;
    private final Attributes additionalAttributes;
    private final Object lock = new Object();
    private transient Attributes attributes;

    public CoreExceptionEvent(Throwable exception, long timestampNanos, Attributes attributes) {
        this.exception = exception;
        this.timestampNanos = timestampNanos;
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

            attributes = mergeExceptionAttributes(additionalAttributes, exception);
            return attributes;
        }
    }

    private Attributes mergeExceptionAttributes(Attributes attributes, Throwable exception) {
        AttributesBuilder builder = additionalAttributes != null ? attributes.toBuilder() : Attributes.builder();
        builder.put(SemanticAttributes.EXCEPTION_TYPE, exception.getClass().getCanonicalName());
        String message = exception.getMessage();
        if (message != null && !message.isEmpty()) {
            builder.put(SemanticAttributes.EXCEPTION_MESSAGE, message);
        }
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        builder.put(SemanticAttributes.EXCEPTION_STACKTRACE, writer.toString());
        return builder.build();
    }
}
