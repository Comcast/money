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

/**
 * A handler that does some processing on a Span.
 * <p>
 *     When a {@link Span} is stopped, the resulting span is passed to the SpanHandler for processing.  Useful
 *     processing includes things like logging the span, or sending the span to Graphite.
 * </p>
 */
public interface SpanHandler {

    /**
     * Handles a span that has been stopped.
     *
     * @param span {@link Span} that has been stopped and is ready for processing
     */
    void handle(Span span);
}
