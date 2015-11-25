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

package com.comcast.money.reflect;

import com.comcast.money.annotations.TracedData;
import com.comcast.money.core.Note;
import com.comcast.money.core.Tracer;

public class TracedDataParameter<T> {
    private final T parameterValue;
    private final Class<T> parameterType;
    private final String name;

    public TracedDataParameter(TracedData tracedData, T parameterValue, Class<T> parameterType) {
        this.name = tracedData.value();
        this.parameterValue = parameterValue;
        this.parameterType = parameterType;
    }

    public void trace(Tracer tracer) {

        tracer.record(getNote());
    }

    public Object getParameterValue() {
        return parameterValue;
    }

    public Class<?> getParameterType() {
        return parameterType;
    }

    public String getName() {
        return name;
    }

    private Note<T> getNote() {
        return new Note<T>(name, parameterValue);
    }
}
