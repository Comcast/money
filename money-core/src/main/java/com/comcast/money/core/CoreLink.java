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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;

import com.comcast.money.api.LinkInfo;

final class CoreLink implements LinkInfo {
    private final SpanContext spanContext;
    private final Attributes attributes;

    public CoreLink(SpanContext spanContext) {
        this(spanContext, Attributes.empty());
    }

    public CoreLink(SpanContext spanContext, Attributes attributes) {
        this.spanContext = spanContext;
        this.attributes = attributes;
    }

    @Override
    public SpanContext spanContext() {
        return spanContext;
    }

    @Override
    public Attributes attributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CoreLink coreLink = (CoreLink) o;

        if (!spanContext.equals(coreLink.spanContext)) return false;
        return attributes.equals(coreLink.attributes);
    }

    @Override
    public int hashCode() {
        int result = spanContext.hashCode();
        result = 31 * result + attributes.hashCode();
        return result;
    }
}
