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

import scala.Function1;

public interface SpanFactory {

    Span newSpan(String spanName);

    Span newSpan(SpanId spanId, String spanName);

    /**
     * Continues a trace by creating a child span from the given x-moneytrace header
     * value.
     *
     * @param childName - the name of the child span to create
     * @param getHeader - function for retrieving value of x-moneytrace header
     * @return a child span with trace id and parent id from trace context header or a new root span if the
     * traceContextHeader is malformed.
     */
    Span newSpanFromHeader(String childName, Function1<String, String> getHeader);

    Span childSpan(String childName, Span span);

    Span childSpan(String childName, Span span, boolean sticky);
}
