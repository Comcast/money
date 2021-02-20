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

import com.comcast.money.api.EventInfo;

class CoreEvent implements EventInfo {
    private final String name;
    private final long timestampNanos;
    private final Attributes attributes;

    public CoreEvent(String name, long timestampNanos) {
        this(name, timestampNanos, Attributes.empty());
    }

    public CoreEvent(String name, long timestampNanos, Attributes attributes) {
        this.name = name;
        this.timestampNanos = timestampNanos;
        this.attributes = attributes;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Attributes attributes() {
        return attributes;
    }

    @Override
    public long timestamp() {
        return timestampNanos;
    }

    @Override
    public Throwable exception() {
        return null;
    }
}
