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

package com.comcast.money.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comcast.money.annotations.Traced;
import com.comcast.money.annotations.TracedData;

@Component
public class SampleTraceBean {

    @Autowired
    private SpringTracer springTracer;

    @Traced("SampleTrace")
    public void doSomethingGood() {

        springTracer.record("foo", "bar", false);
    }

    @Traced("SampleTrace")
    public void doSomethingBad() {

        springTracer.record("foo", "bar", false);
        throw new IllegalStateException("fail");
    }

    @Traced("SampleTrace")
    public void doSomethingWithTracedParams(
            @TracedData("STRING") String str,
            @TracedData("BOOLEAN") Boolean bool,
            @TracedData("LONG") Long lng,
            @TracedData("DOUBLE") Double dbl) {

        return;
    }

    @Traced(
        value="SampleTrace",
        ignoredExceptions = { IllegalArgumentException.class }
    )
    public void doSomethingButIgnoreException() {

        springTracer.record("foo", "bar", false);
        throw new IllegalArgumentException("fail");
    }

    public void doSomethingNotTraced() {
    }
}
