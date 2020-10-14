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

import io.opentelemetry.common.AttributeKey;
import io.opentelemetry.common.AttributeType;

/**
 * A note that has been recorded on a {@link Span}
 *
 * @param <T> The type of Note.  This is currently limited to Long, String, Boolean and Double
 */
public class Note<T> {

    private final AttributeKey<T> key;
    private final T value;
    private final long timestamp;
    private final boolean sticky; // indicates that this note should be sent to child spans if so requested by user

    private Note(AttributeKey<T> key, T value, Long timestamp, boolean sticky) {
        this.key = key;
        this.value = value;
        this.timestamp = timestamp;
        this.sticky = sticky;
    }

    /**
     * @return The key of the note.
     */
    public AttributeKey<T> key() {
        return key;
    }

    /**
     * @return The name of the note
     */
    public String name() {
        return key.getKey();
    }

    /**
     * @return The type of the type.
     */
    public AttributeType type() {
        return key.getType();
    }

    /**
     * @return {@code true} if the value of the note is {@code null}; otherwise, {@code false}
     */
    public boolean isNull() {
        return value == null;
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
        return of(AttributeKey.stringKey(name), value);
    }

    /**
     * Creates a new note that contains a long
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a long value
     */
    public static Note<Long> of(String name, long value) {
        return of(AttributeKey.longKey(name), value);
    }

    /**
     * Creates a new note that contains a boolean
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a boolean value
     */
    public static Note<Boolean> of(String name, boolean value) {
        return of(AttributeKey.booleanKey(name), value);
    }

    /**
     * Creates a new note that contains a double
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a double value
     */
    public static Note<Double> of(String name, double value) {
        return of(AttributeKey.doubleKey(name), value);
    }

    /**
     * Creates a new note that contains a string
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @return A new {@link Note} that contains a String value
     */
    public static Note<String> of(String name, String value, boolean sticky) {
        return of(AttributeKey.stringKey(name), value, sticky);
    }

    /**
     * Creates a new note that contains a long
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @return A new {@link Note} that contains a long value
     */
    public static Note<Long> of(String name, long value, boolean sticky) {
        return of(AttributeKey.longKey(name), value, sticky);
    }

    /**
     * Creates a new note that contains a boolean
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @return A new {@link Note} that contains a boolean value
     */
    public static Note<Boolean> of(String name, boolean value, boolean sticky) {
        return of(AttributeKey.booleanKey(name), value, sticky);
    }

    /**
     * Creates a new note that contains a double
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @return A new {@link Note} that contains a double value
     */
    public static Note<Double> of(String name, double value, boolean sticky) {
        return of(AttributeKey.doubleKey(name), value, sticky);
    }

    /**
     * Creates a new note that contains a string
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @param timestamp The timestamp for the note
     * @return A new {@link Note} that contains a String value
     */
    public static Note<String> of(String name, String value, boolean sticky, long timestamp) {
        return of(AttributeKey.stringKey(name), value, sticky, timestamp);
    }

    /**
     * Creates a new note that contains a long
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @param timestamp The timestamp for the note
     * @return A new {@link Note} that contains a long value
     */
    public static Note<Long> of(String name, long value, boolean sticky, long timestamp) {
        return of(AttributeKey.longKey(name), value, sticky, timestamp);
    }

    /**
     * Creates a new note that contains a boolean
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @param timestamp The timestamp for the note
     * @return A new {@link Note} that contains a boolean value
     */
    public static Note<Boolean> of(String name, boolean value, boolean sticky, long timestamp) {
        return of(AttributeKey.booleanKey(name), value, sticky, timestamp);
    }

    /**
     * Creates a new note that contains a double
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @param timestamp The timestamp for the note
     * @return A new {@link Note} that contains a double value
     */
    public static Note<Double> of(String name, double value, boolean sticky, long timestamp) {
        return of(AttributeKey.doubleKey(name), value, sticky, timestamp);
    }

    /**
     * Creates a new note that contains a string
     * @param name The name of the note
     * @param value The value for the note
     * @param timestamp The timestamp for the note
     * @return A new {@link Note} that contains a String value
     */
    public static Note<String> of(String name, String value, long timestamp) {
        return of(AttributeKey.stringKey(name), value, timestamp);
    }

    /**
     * Creates a new note that contains a long
     * @param name The name of the note
     * @param value The value for the note
     * @param timestamp The timestamp for the note
     * @return A new {@link Note} that contains a long value
     */
    public static Note<Long> of(String name, long value, long timestamp) {
        return of(AttributeKey.longKey(name), value, timestamp);
    }

    /**
     * Creates a new note that contains a boolean
     * @param name The name of the note
     * @param value The value for the note
     * @param timestamp The timestamp for the note
     * @return A new {@link Note} that contains a boolean value
     */
    public static Note<Boolean> of(String name, boolean value, long timestamp) {
        return of(AttributeKey.booleanKey(name), value, timestamp);
    }

    /**
     * Creates a new note that contains a double
     * @param name The name of the note
     * @param value The value for the note
     * @param timestamp The timestamp for the note
     * @return A new {@link Note} that contains a double value
     */
    public static Note<Double> of(String name, double value, long timestamp) {
        return of(AttributeKey.doubleKey(name), value, timestamp);
    }

    /**
     * Creates a new note that contains the type specified by the attribute key
     * @param attributeKey The typed attribute key of the note
     * @param value The value for the note
     * @param <T> The type of the type
     * @return A new {@link Note} that contains the value
     */
    public static <T> Note<T> of(AttributeKey<T> attributeKey, T value) {
        return of(attributeKey, value, false, System.currentTimeMillis());
    }

    /**
     * Creates a new note that contains the type specified by the attribute key
     * @param attributeKey The typed attribute key of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @param <T> The type of the type
     * @return A new {@link Note} that contains the value
     */
    public static <T> Note<T> of(AttributeKey<T> attributeKey, T value, boolean sticky) {
        return of(attributeKey, value, sticky, System.currentTimeMillis());
    }

    /**
     * Creates a new note that contains the type specified by the attribute key
     * @param attributeKey The typed attribute key of the note
     * @param value The value for the note
     * @param timestamp The timestamp for the note
     * @param <T> The type of the type
     * @return A new {@link Note} that contains the value
     */
    public static <T> Note<T> of(AttributeKey<T> attributeKey, T value, long timestamp) {
        return of(attributeKey, value, false, timestamp);
    }

    /**
     * Creates a new note that contains the type specified by the attribute key
     * @param attributeKey The typed attribute key of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @param timestamp The timestamp for the note
     * @param <T> The type of the type
     * @return A new {@link Note} that contains the value
     */
    public static <T> Note<T> of(AttributeKey<T> attributeKey, T value, boolean sticky, long timestamp) {
        return new Note<>(attributeKey, value, timestamp, sticky);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Note<?> note = (Note<?>) o;

        if (timestamp != note.timestamp) return false;
        if (sticky != note.sticky) return false;
        if (!key.equals(note.key)) return false;
        return Objects.equals(value, note.value);

    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (sticky ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Note(");
        builder.append("name=").append(key.getKey());
        builder.append(", value=").append(value == null ? "null" : value.toString());
        builder.append(", timestamp=").append(timestamp);
        builder.append(", sticky=").append(sticky);
        builder.append(")");
        return builder.toString();
    }
}
