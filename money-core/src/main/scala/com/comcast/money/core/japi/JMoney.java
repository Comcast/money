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

package com.comcast.money.core.japi;

import io.opentelemetry.common.AttributeKey;
import io.opentelemetry.context.Scope;

import com.comcast.money.api.Note;
import com.comcast.money.api.Span;
import com.comcast.money.core.Money;
import com.comcast.money.core.Tracer;

/**
 * Java API for working directly with money from Java code
 */
public class JMoney {
    private static Tracer tracer = Money.Environment().tracer();

    private JMoney() { }

    public static boolean isEnabled() {

        return Money.Environment().enabled();
    }

    /**
     * @return the {@link com.comcast.money.core.Tracer} to use for tracing
     */
    public static Tracer tracer() {
        return tracer;
    }

    protected static void setTracer(Tracer tracer) {
        if (tracer != null) {
            JMoney.tracer = tracer;
        } else {
            JMoney.tracer = Money.Environment().tracer();
        }
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
     * @param propagate whether or not to propagate note to children spans
     */
    public static void record(String noteName, Double value, boolean propagate) {

        tracer().record(noteName, value, propagate);
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
     * @param propagate whether or not to propagate note to children spans
     */
    public static void record(String noteName, String value, boolean propagate) {

        tracer().record(noteName, value, propagate);
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
     * @param propagate whether or not to propagate note to children spans
     */
    public static void record(String noteName, Long value, boolean propagate) {

        tracer().record(noteName, value, propagate);
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
     * @param propagate whether or not to propagate note to children spans
     */
    public static void record(String noteName, boolean value, boolean propagate) {

        tracer().record(noteName, value, propagate);
    }

    /**
     * Records a note in the current trace span if one is present.  If the Object value that is provided
     * is null; or if it is a type other than String, Long, Double, or Boolean, then we record a
     * String Note, and perform a "toString" on the value.
     * @param noteName The name of the note to record
     * @param value The value to be recorded on the note
     * @param propagate whether or not to propagate the note to child spans
     */
    public static void record(String noteName, Object value, boolean propagate) {

        tracer().record(toNote(noteName, value, propagate));
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
     * Records a note in the current trace span if one is present.
     * @param key The name and type of the attribute to record
     * @param value The value to be recorded on the note
     */
    public static <T> void record(AttributeKey<T> key, T value) {
        tracer().record(Note.of(key, value));
    }

    /**
     * Records a note in the current trace span if one is present.
     * @param key The name and type of the attribute to record
     * @param value The value to be recorded on the note
     * @param propagate whether or not to propagate note to children spans
     */
    public static <T> void record(AttributeKey<T> key, T value, boolean propagate) {
        tracer().record(Note.of(key, value, propagate));
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
     * You should call {@link Scope#close()} stop the timer.
     *
     * If you do not stop the timer, then the duration of the timer will be whenever
     * the current Span is stopped
     * @param noteName The name of the note that will be recorded for the timer
     */
    public static Scope startTimer(String noteName) {

        return tracer().startTimer(noteName);
    }

    /**
     * Stops a timer that was previously started on the current span.
     *
     * This will add a note to the span with the name provided.  The value will be
     * the duration between the startTimer and stopTimer invocations.
     * @param noteName The name of the timer to stop
     * @deprecated Close the {@link Scope} returned by {@link JMoney#startTimer(String)}
     */
    @Deprecated
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
     * @return the created span
     */
    public static Span startSpan(String spanName) {

        return tracer().startSpan(spanName);
    }

    /**
     * Creates a new {@link Span.Builder} for configuring and creating a new span with the given name.
     *
     * If a span already exists, the new span will be a child span of the existing span.
     *
     * If no span exists, the new span will be a root span
     *
     * @param spanName The name of the span
     * @return the {@link Span.Builder}
     */
    public static Span.Builder spanBuilder(String spanName) {

        return tracer().spanBuilder(spanName);
    }

    /**
     * Stops the existing span if one is present.
     *
     * Once the span is stopped, the data for the span will be emitted
     * @param success An indicator of whether or not the span was successful
     * @deprecated Close the {@lin Scope} returned from {@link JMoney#startSpan(String)}
     */
    @Deprecated
    public static void stopSpan(boolean success) {

        tracer().stopSpan(success);
    }

    /**
     * Stops the existing span if one is present.  Assumes a result of success
     * @deprecated Close the {@lin Scope} returned from {@link JMoney#startSpan(String)}
     */
    @Deprecated
    public static void stopSpan() {
        tracer().stopSpan(true);
    }

    /**
     * Creates a new span that can be used in Java that is closeable.  Once the try
     * block completes the span will automatically be stopped.
     *
     * <pre>
     *     try (JMoney.TraceSpan span = JMoney.newSpan("some-name")) {
     *         JMoney.record("some", 1);
     *     }
     * </pre>
     * <pre>
     *     try (JMoney.TraceSpan span = JMoney.newSpan("some-name")) {
     *         try {
     *             // do something here
     *         } catch (Exception exception) {
     *             span.fail();
     *             // log exception here
     *         }
     *     }
     * </pre>
     * @param spanName The name of the span
     * @return a {@link com.comcast.money.core.japi.JMoney.TraceSpan} that really can only be closed to stop the span
     */
    public static TraceSpan newSpan(String spanName) {

        return newSpan(spanName, true);
    }

    /**
     * Creates a new span that can be used in Java that is closeable.  Once the try
     * block completes the span will automatically be stopped with the specified
     * success indicator unless changed on the returned span.
     *
     * <pre>
     *     try (JMoney.TraceSpan span = JMoney.newSpan("some-name", false)) {
     *         JMoney.record("some, 1);
     *         span.success();
     *     }
     * </pre>
     * @param spanName The name of the span
     * @param success An indicator of whether or not the span was successful by default
     * @return a {@link com.comcast.money.core.japi.JMoney.TraceSpan} that really can only be closed to stop the span
     */
    public static TraceSpan newSpan(String spanName, boolean success) {
        startSpan(spanName);
        return new TraceSpan(success);
    }

    /**
     * Starts a new timer that can be used in Java that is closeable.  Once the try block
     * completes the timer will automatically be stopped.
     *
     * <pre>
     *     try (JMoney.TraceTimer timer = JMoney.newTimer("timer-name")) {
     *         // do something here
     *     }
     * </pre>
     * @param timerName The name of the timer
     * @return a {@link com.comcast.money.core.japi.JMoney.TraceTimer} that really can only be closed to stop the timer
     */
    public static TraceTimer newTimer(String timerName) {

        startTimer(timerName);
        return new TraceTimer(timerName);
    }

    /**
     * Creates a span to measure the computation of a task which may throw an exception
     *
     * <pre>
     *     JMoney.trace("span-name", () -> {
     *         // do something here
     *     });
     * </pre>
     * @param spanName The name of the span
     * @param runnable The task to complete
     * @param <E> the type of checked exception
     * @throws E if unable to complete the task
     */
    public static <E extends Exception> void trace(String spanName, CheckedRunnable<E> runnable) throws E {
        try (TraceSpan span = newSpan(spanName, false)) {
            runnable.run();
            span.success();
        }
    }

    /**
     * Creates a span to measure the computation of a task which may throw an exception
     *
     * <pre>
     *     JMoney.trace("span-name", span -> {
     *         try {
     *             // do something here
     *         } catch (Exception exception) {
     *             span.fail();
     *         }
     *     });
     * </pre>
     * @param spanName The name of the span
     * @param consumer The task to complete
     * @param <E> the type of checked exception
     * @throws E if unable to complete the task
     */
    public static <E extends Exception> void trace(String spanName, CheckedConsumer<TraceSpan, E> consumer) throws E {
        try (TraceSpan span = newSpan(spanName)) {
            try {
                consumer.accept(span);
            } catch (Exception exception) {
                span.fail();
                @SuppressWarnings("unchecked")
                E checked = (E) exception;
                throw checked;
            }
        }
    }

    /**
     * Creates a span to measure the computation of a task which returns a result and may throw an exception
     *
     * <pre>
     *     String value = JMoney.trace("span-name", () -> {
     *         // do something here
     *         return "Result";
     *     });
     * </pre>
     * @param spanName The name of the span
     * @param callable The task to compute the result
     * @param <V> the type of the result
     * @param <E> the type of checked exception
     * @return the computed result of the task
     * @throws E if unable to complete the task
     */
    public static <V, E extends Exception> V trace(String spanName, CheckedCallable<V, E> callable) throws E {
        try (TraceSpan span = newSpan(spanName, false)) {
            V result = callable.call();
            span.success();
            return result;
        }
    }

    /**
     * Creates a span to measure the computation of a task which returns a result and may throw an exception
     *
     * <pre>
     *     String value = JMoney.trace("span-name", span -> {
     *         String result = calculateSomeResult();
     *         if (result == null) {
     *             span.fail();
     *         }
     *         return result;
     *     });
     * </pre>
     * @param spanName The name of the span
     * @param function The task to compute the result
     * @param <V> the type of the result
     * @param <E> the type of checked exception
     * @return the computed result of the task
     * @throws E if unable to complete the task
     */
    public static <V, E extends Exception> V trace(String spanName, CheckedFunction<TraceSpan, V, E> function) throws E {
        try (TraceSpan span = newSpan(spanName)) {
            try {
                return function.apply(span);
            } catch (Exception exception) {
                span.fail();
                @SuppressWarnings("unchecked")
                E checked = (E) exception;
                throw checked;
            }
        }
    }

    /**
     * Creates a timer to measure the duration of a task
     *
     * <pre>
     *     JMoney.time("timer-name", () -> {
     *         // do something here
     *     });
     * </pre>
     * @param timerName The name of the timer
     * @param runnable The task to be timed
     * @param <E> the type of checked exception
     * @throws E if unable to complete the task
     */
    public static <E extends Exception> void time(String timerName, CheckedRunnable<E> runnable) throws E {
        try (TraceTimer timer = newTimer(timerName)) {
            runnable.run();
        }
    }

    /**
     * Creates a timer to measure the duration of a task that computes a result
     *
     * <pre>
     *     String result = JMoney.time("timer-name", () -> {
     *         // do something here
     *         return "Result";
     *     });
     * </pre>
     * @param timerName The name of the timer
     * @param callable The task to be timed
     * @param <V> the type of the result
     * @param <E> the type of checked exception
     * @return the computed result of the task
     * @throws E if unable to complete the task
     */
    public static <V, E extends Exception> V time(String timerName, CheckedCallable<V, E> callable) throws E {
        try (TraceTimer timer = newTimer(timerName)) {
            return callable.call();
        }
    }

    /**
     * A {@link java.lang.AutoCloseable} that has a close method that will stop the current span
     */
    public static class TraceSpan implements AutoCloseable {

        private boolean success;
        private boolean stopped = false;

        public TraceSpan() {
            this(true);
        }

        public TraceSpan(boolean success) {
            this.success = success;
        }

        public void fail() {
            success = false;
        }

        public void success() {
            success = true;
        }

        @Override public void close() {
            if (!stopped) {
                stopped = true;
                stopSpan(success);
            }
        }
    }

    /**
     * A {@link java.lang.AutoCloseable} that has a close method that will stop the timer
     */
    public static class TraceTimer implements AutoCloseable {

        private final String noteName;
        private boolean stopped = false;

        TraceTimer(String noteName) {
            this.noteName = noteName;
        }

        @Override
        public void close() {
            if (!stopped) {
                stopped = true;
                stopTimer(noteName);
            }
        }
    }

    /**
     * A task that returns a result and may throw an exception.
     * @param <V> the type of the result
     * @param <E> the type of checked exception
     */
    @FunctionalInterface
    public interface CheckedCallable<V, E extends Exception> {
        /**
         * Computes a result or throws a checked exception
         * @return the computed result
         * @throws E if unable to compute a result
         */
        V call() throws E;
    }

    /**
     * A task that does not return a result and may throw an exception
     * @param <E> the type of checked exception
     */
    @FunctionalInterface
    public interface CheckedRunnable<E extends Exception> {
        /**
         * Runs a task or throws a checked exception
         * @throws E if unable to complete the task
         */
        void run() throws E;
    }

    /**
     * A task that accepts a single input, does not return a result and may throw an exception
     * @param <V> the type of the input to the task
     * @param <E> the type of checked exception
     */
    @FunctionalInterface
    public interface CheckedConsumer<V, E extends Exception> {
        /**
         * Runs a task with the accepted input
         * @param value the input argument
         * @throws E if unable to complete the task
         */
        void accept(V value) throws E;
    }

    /**
     * A task that accepts a single input, returns a result and may throw an exception
     * @param <V> the type of the input to the task
     * @param <R> the type of the result
     * @param <E> the type of checked exception
     */
    @FunctionalInterface
    public interface CheckedFunction<V, R, E extends Exception> {
        /**
         * Runs a task with the accepted input and computes a result
         * @param value the input argument
         * @return the computed result
         * @throws E if unable to complete the task
         */
        R apply(V value) throws E;
    }

    private static Note<?> toNote(String name, Object value, boolean propagate) {

        if (value == null) {
            return Note.of(name, null, propagate);
        } else if (value instanceof Boolean) {
            Boolean bool = (Boolean)value;
            return Note.of(name, bool, propagate);
        } else if (value instanceof Double) {
            Double dbl = (Double)value;
            return Note.of(name, dbl, propagate);
        } else if (value instanceof Long) {
            Long lng = (Long)value;
            return Note.of(name, lng, propagate);
        } else {
            return Note.of(name, value.toString(), propagate);
        }
    }
}
