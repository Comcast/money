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
 * A note that has been recorded on a {@link Span}
 *
 * @param <T> The type of Note.  This is currently limited to Long, String, Boolean and Double
 */
public class Note<T> {

    private final String name;
    private final T value;
    private final long timestamp;
    private final boolean sticky; // indicates that this note should be sent to child spans if so requested by user

    private Note(String name, T value) {
        this(name, value, System.currentTimeMillis(), false);
    }

    private Note(String name, T value, boolean sticky) {
        this(name, value, System.currentTimeMillis(), sticky);
    }

    private Note(String name, T value, Long timestamp, boolean sticky) {
        this.name = name;
        this.value = value;
        this.timestamp = timestamp;
        this.sticky = sticky;
    }

    /**
     * @return The name of the note
     */
    public String name() {
        return name;
    }

    /**
     * @return The value for the note
     */
    public T value() {
        return value;
    }

    /**
     * @return The timestamp in milliseconds when the note was created
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     * Stickiness indicates that this note should be passed down to child spans.  It becomes useful
     * in certain logging situations where a consistent piece of state is maintained throughout the duration
     * of a trace.
     *
     * @return an indicator of whether or not this note is "sticky".
     */
    public boolean isSticky() {
        return sticky;
    }

    /**
     * Creates a new note that contains a string
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a String value
     */
    public static Note<String> of(String name, String value) {
        return new Note<String>(name, value);
    }

    /**
     * Creates a new note that contains a long
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a long value
     */
    public static Note<Long> of(String name, long value) {
        return new Note<Long>(name, value);
    }

    /**
     * Creates a new note that contains a boolean
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a boolean value
     */
    public static Note<Boolean> of(String name, boolean value) {
        return new Note<Boolean>(name, value);
    }

    /**
     * Creates a new note that contains a double
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a double value
     */
    public static Note<Double> of(String name, double value) {
        return new Note<Double>(name, value);
    }

    /**
     * Creates a new note that contains a string
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @return A new {@link Note} that contains a String value
     */
    public static Note<String> of(String name, String value, boolean sticky) {
        return new Note<String>(name, value, sticky);
    }

    /**
     * Creates a new note that contains a long
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @return A new {@link Note} that contains a long value
     */
    public static Note<Long> of(String name, long value, boolean sticky) {
        return new Note<Long>(name, value, sticky);
    }

    /**
     * Creates a new note that contains a boolean
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @return A new {@link Note} that contains a boolean value
     */
    public static Note<Boolean> of(String name, boolean value, boolean sticky) {
        return new Note<Boolean>(name, value, sticky);
    }

    /**
     * Creates a new note that contains a double
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @return A new {@link Note} that contains a double value
     */
    public static Note<Double> of(String name, double value, boolean sticky) {
        return new Note<Double>(name, value, sticky);
    }
}
