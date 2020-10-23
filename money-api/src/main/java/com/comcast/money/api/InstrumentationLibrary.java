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

import java.util.Objects;

public final class InstrumentationLibrary {
    public static final InstrumentationLibrary UNKNOWN = new InstrumentationLibrary("unknown");

    private final String name;
    private final String version;

    public InstrumentationLibrary(String name) {
        this(name, null);
    }

    public InstrumentationLibrary(String name, String version) {
        Objects.requireNonNull(name);
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrumentationLibrary that = (InstrumentationLibrary) o;

        if (!name.equals(that.name)) return false;
        return Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (version != null) {
            return "{name='" + name + "', version='" + version + "'}";
        } else {
            return "{name='" + name + "'}";
        }

    }
}
