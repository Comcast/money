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
 * A Span is a container that represents a unit of work.  It could be a long running operation or sequence of
 * statements in process, or a remote system call.
 *
 * A Span is immutable, all changes to the span result in a new Span being created.
 */
public interface Span {

    /**
     * Signals the span that it has started
     */
    void start();

    /**
     * Stops the span asserts a successful result
     */
    void stop();

    /**
     * Ends a span, moving it to a Stopped state
     * @param result The result of the span (success or failure)
     */
    void stop(Boolean result);

    /**
     * Records a given note onto the span.  If the note was already present, it will be overwritten
     * @param note
     */
    void record(Note<?> note);

    /**
     * Starts a new timer on the span
     * @param timerKey The name of the timer to start
     * @return a new Span with the timer started
     */
    void startTimer(String timerKey);

    /**
     * Stops an existing timer on the span
     * @param timerKey The name of the timer
     * @return a new Span with the timer stopped
     */
    void stopTimer(String timerKey);

    /**
     * @return The current state of the Span
     */
    SpanInfo info();
}
