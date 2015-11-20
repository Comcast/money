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

public class Note<T> {

    private final String name;
    private final T value;
    private final Long timestamp;
    private final boolean propagate;

    public Note(String name, T value) {
        this(name, value, System.currentTimeMillis(), false);
    }

    public Note(String name, T value, boolean propagate) {
        this(name, value, System.currentTimeMillis(), propagate);
    }

    public Note(String name, T value, Long timestamp, boolean propagate) {
        this.name = name;
        this.value = value;
        this.timestamp = timestamp;
        this.propagate = propagate;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public boolean isPropagate() {
        return propagate;
    }
}
