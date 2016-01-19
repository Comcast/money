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

package com.comcast.money.japi;

import akka.actor.ActorSystem;
import scala.Option;

import com.comcast.money.api.Note;
import com.comcast.money.core.Metrics;
import com.comcast.money.core.Money$;
import com.comcast.money.core.Result;
import com.comcast.money.core.Tracer;
import com.comcast.money.util.DateTimeUtil;

/**
 * Java API for working directly with money from Java code
 */
public class JMoney {
    /**
     * @return the [[com.comcast.money.core.Tracer]] to use for tracing
     */
    public static boolean isEnabled() { return Money$.MODULE$.isEnabled(); }
    public static ActorSystem actorSystem() {
        return Money$.MODULE$.actorSystem();
    }
    public static Tracer tracer() {
        return Money$.MODULE$.tracer();
    }
    public static Metrics metrics() {
        return Money$.MODULE$.metrics();
    }

    /**
     * Records a note in the current trace span if one is present
     * @param noteName The name of the note to record
     * @param value The value to be recorded on the note
     */
    public static void record(String noteName, Double value) {

        tracer().record(noteName, value, false);
    }

    /**
     * Records a note in the current trace span if one is present
     * @param noteName The name of the note to record
     * @param value The value to be recorded on the note
     * @param sticky whether or not to sticky note to children spans
     */
    public static void record(String noteName, Double value, boolean sticky) {

        tracer().record(noteName, value, sticky);
    }

    /**
     * Records a note in the current trace span if one is present
     * @param noteName The name of the note to record
     * @param value The value to be recorded on the note
     */
    public static void record(String noteName, String value) {

        tracer().record(noteName, value, false);
    }

    /**
     * Records a note in the current trace span if one is present
     * @param noteName The name of the note to record
     * @param value The value to be recorded on the note
     * @param sticky whether or not to sticky note to children spans
     */
    public static void record(String noteName, String value, boolean sticky) {

        tracer().record(noteName, value, sticky);
    }

    /**
     * Records a note in the current trace span if one is present
     * @param noteName The name of the note to record
     * @param value The value to be recorded on the note
     */
    public static void record(String noteName, Long value) {

        tracer().record(noteName, value, false);
    }

    /**
     * Records a note in the current trace span if one is present
     * @param noteName The name of the note to record
     * @param value The value to be recorded on the note
     * @param sticky whether or not to sticky note to children spans
     */
    public static void record(String noteName, Long value, boolean sticky) {

        tracer().record(noteName, value, sticky);
    }

    /**
     * Records a note in the current trace span if one is present
     * @param noteName The name of the note to record
     * @param value The value to be recorded on the note
     */
    public static void record(String noteName, boolean value) {

        tracer().record(noteName, value, false);
    }

    /**
     * Records a note in the current trace span if one is present
     * @param noteName The name of the note to record
     * @param value The value to be recorded on the note
     * @param sticky whether or not to sticky note to children spans
     */
    public static void record(String noteName, boolean value, boolean sticky) {

        tracer().record(noteName, value, sticky);
    }

    /**
     * Records a note in the current trace span if one is present.  If the Object value that is provided
     * is null; or if it is a type other than String, Long, Double, or Boolean, then we record a
     * String Note, and perform a "toString" on the value.
     * @param noteName The name of the note to record
     * @param value The value to be recorded on the note
     * @param sticky whether or not to sticky the note to child spans
     */
    public static void record(String noteName, Object value, boolean sticky) {

        tracer().record(toNote(noteName, value, sticky));
    }

    /**
     * Records a note in the current trace span if one is present.  If the Object value that is provided
     * is null; or if it is a type other than String, Long, Double, or Boolean, then we record a
     * String Note, and perform a "toString" on the value.
     * @param noteName The name of the note to record
     * @param value The value to be recorded on the note
     */
    public static void record(String noteName, Object value) {

        record(noteName, value, false);
    }

    /**
     * Records a note in the current trace span if one is present
     *
     * This will store the current timestamp as the value of the note
     * @param noteName The name of the note to record
     */
    public static void record(String noteName) {

        tracer().time(noteName);
    }

    /**
     * Starts a timer that will be recorded on the current span
     *
     * You should call stopTimer with the same noteName value to stop the timer.
     *
     * If you do not call stopTimer, then the duration of the timer will be whenever
     * the current Span is stopped
     * @param noteName The name of the note that will be recorded for the timer
     */
    public static void startTimer(String noteName) {

        tracer().startTimer(noteName);
    }

    /**
     * Stops a timer that was previously started on the current span.
     *
     * This will add a note to the span with the name provided.  The value will be
     * the duration between the startTimer and stopTimer invocations.
     * @param noteName The name of the timer to stop
     */
    public static void stopTimer(String noteName) {

        tracer().stopTimer(noteName);
    }

    /**
     * Starts a new span with the given name.
     *
     * If a span already exists, the new span will be a child span of the existing span.
     *
     * If no span exists, the new span will be a root span
     *
     * Use this method carefully, you must be certain to stop the span.  If you do not stop the span,
     * it will eventually timeout and no data will be recorded.  Under heavy volume, this will
     * add memory pressure to the process
     *
     * @param spanName The name of the span
     */
    public static void startSpan(String spanName) {

        tracer().startSpan(spanName);
    }

    /**
     * Stops the existing span if one is present.
     *
     * Once the span is stopped, the data for the span will be emitted
     * @param success An indicator of whether or not the span was successful
     */
    public static void stopSpan(boolean success) {

        if (success) {
            tracer().stopSpan(Result.success());
        } else {
            tracer().stopSpan(Result.failed());
        }
    }

    /**
     * Stops the existing span if one is present.  Assumes a result of success
     */
    public static void stopSpan() {
        tracer().stopSpan(Result.success());
    }

    /**
     * Creates a new span that can be used in Java that is closeable.  Once the try
     * block completes, the span will automatically be stopped.
     *
     * <pre>
     *     try(TraceSpan span = JMoney.newSpan("some-name")) {
     *         JMoney.record("some", 1);
     *     }
     * </pre>
     * @param spanName The name of the span
     * @return a {@link com.comcast.money.japi.JMoney.TraceSpan} that really can only be closed to stop the span
     */
    public static TraceSpan newSpan(String spanName) {

        startSpan(spanName);
        return new TraceSpan();
    }

    /**
     * A {@link java.lang.AutoCloseable} that has a close method that will stop the current span
     */
    public static class TraceSpan implements AutoCloseable {

        private boolean success = true;

        public void fail() {
            success = false;
        }

        public void close() throws Exception {
            stopSpan(success);
        }
    }

    private static Note<?> toNote(String name, Object value, Boolean sticky) {

        if (value == null) {
            return Note.of(name, null, sticky, DateTimeUtil.microTime());
        } else if (value instanceof Boolean) {
            Boolean bool = (Boolean)value;
            return Note.of(name, bool, sticky, DateTimeUtil.microTime());
        } else if (value instanceof Double) {
            Double dbl = (Double)value;
            return Note.of(name, dbl, sticky, DateTimeUtil.microTime());
        } else if (value instanceof Long) {
            Long lng = (Long)value;
            return Note.of(name, lng, sticky, DateTimeUtil.microTime());
        } else {
            return Note.of(name, value.toString(), sticky, DateTimeUtil.microTime());
        }
    }
}
