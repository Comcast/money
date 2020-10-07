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

import java.util.List;
import java.util.Objects;

import io.opentelemetry.common.AttributeValue;

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
        return new Note<>(name, value);
    }

    /**
     * Creates a new note that contains a long
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a long value
     */
    public static Note<Long> of(String name, long value) {
        return new Note<>(name, value);
    }

    /**
     * Creates a new note that contains a boolean
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a boolean value
     */
    public static Note<Boolean> of(String name, boolean value) {
        return new Note<>(name, value);
    }

    /**
     * Creates a new note that contains a double
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a double value
     */
    public static Note<Double> of(String name, double value) {
        return new Note<>(name, value);
    }

    /**
     * Creates a new note that contains a string
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a String value
     */
    public static Note<List<String>> ofStrings(String name, List<String> value) {
        return new Note<>(name, value);
    }

    /**
     * Creates a new note that contains a long
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a long value
     */
    public static Note<List<Long>> ofLongs(String name, List<Long> value) {
        return new Note<>(name, value);
    }

    /**
     * Creates a new note that contains a boolean
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a boolean value
     */
    public static Note<List<Boolean>> ofBooleans(String name, List<Boolean> value) {
        return new Note<>(name, value);
    }

    /**
     * Creates a new note that contains a double
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a double value
     */
    public static Note<List<Double>> ofDoubles(String name, List<Double> value) {
        return new Note<>(name, value);
    }

    /**
     * Creates a new note that contains a string
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @return A new {@link Note} that contains a String value
     */
    public static Note<String> of(String name, String value, boolean sticky) {
        return new Note<>(name, value, sticky);
    }

    /**
     * Creates a new note that contains a long
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @return A new {@link Note} that contains a long value
     */
    public static Note<Long> of(String name, long value, boolean sticky) {
        return new Note<>(name, value, sticky);
    }

    /**
     * Creates a new note that contains a boolean
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @return A new {@link Note} that contains a boolean value
     */
    public static Note<Boolean> of(String name, boolean value, boolean sticky) {
        return new Note<>(name, value, sticky);
    }

    /**
     * Creates a new note that contains a double
     * @param name The name of the note
     * @param value The value for the note
     * @param sticky Indicates whether this Note should be sticky
     * @return A new {@link Note} that contains a double value
     */
    public static Note<Double> of(String name, double value, boolean sticky) {
        return new Note<>(name, value, sticky);
    }

    /**
     * Creates a new note that contains a string
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a String value
     */
    public static Note<List<String>> ofStrings(String name, List<String> value, boolean sticky) {
        return new Note<>(name, value, sticky);
    }

    /**
     * Creates a new note that contains a long
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a long value
     */
    public static Note<List<Long>> ofLongs(String name, List<Long> value, boolean sticky) {
        return new Note<>(name, value, sticky);
    }

    /**
     * Creates a new note that contains a boolean
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a boolean value
     */
    public static Note<List<Boolean>> ofBooleans(String name, List<Boolean> value, boolean sticky) {
        return new Note<>(name, value, sticky);
    }

    /**
     * Creates a new note that contains a double
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a double value
     */
    public static Note<List<Double>> ofDoubles(String name, List<Double> value, boolean sticky) {
        return new Note<>(name, value, sticky);
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
        return new Note<>(name, value, timestamp, sticky);
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
        return new Note<>(name, value, timestamp, sticky);
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
        return new Note<>(name, value, timestamp, sticky);
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
        return new Note<>(name, value, timestamp, sticky);
    }

    /**
     * Creates a new note that contains a string
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a String value
     */
    public static Note<List<String>> ofStrings(String name, List<String> value, boolean sticky, long timestamp) {
        return new Note<>(name, value, timestamp, sticky);
    }

    /**
     * Creates a new note that contains a long
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a long value
     */
    public static Note<List<Long>> ofLongs(String name, List<Long> value, boolean sticky, long timestamp) {
        return new Note<>(name, value, timestamp, sticky);
    }

    /**
     * Creates a new note that contains a boolean
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a boolean value
     */
    public static Note<List<Boolean>> ofBooleans(String name, List<Boolean> value, boolean sticky, long timestamp) {
        return new Note<>(name, value, timestamp, sticky);
    }

    /**
     * Creates a new note that contains a double
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a double value
     */
    public static Note<List<Double>> ofDoubles(String name, List<Double> value, boolean sticky, long timestamp) {
        return new Note<>(name, value, timestamp, sticky);
    }

    /**
     * Creates a new note that contains a string
     * @param name The name of the note
     * @param value The value for the note
     * @param timestamp The timestamp for the note
     * @return A new {@link Note} that contains a String value
     */
    public static Note<String> of(String name, String value, long timestamp) {
        return new Note<>(name, value, timestamp, false);
    }

    /**
     * Creates a new note that contains a long
     * @param name The name of the note
     * @param value The value for the note
     * @param timestamp The timestamp for the note
     * @return A new {@link Note} that contains a long value
     */
    public static Note<Long> of(String name, long value, long timestamp) {
        return new Note<>(name, value, timestamp, false);
    }

    /**
     * Creates a new note that contains a boolean
     * @param name The name of the note
     * @param value The value for the note
     * @param timestamp The timestamp for the note
     * @return A new {@link Note} that contains a boolean value
     */
    public static Note<Boolean> of(String name, boolean value, long timestamp) {
        return new Note<>(name, value, timestamp, false);
    }

    /**
     * Creates a new note that contains a double
     * @param name The name of the note
     * @param value The value for the note
     * @param timestamp The timestamp for the note
     * @return A new {@link Note} that contains a double value
     */
    public static Note<Double> of(String name, double value, long timestamp) {
        return new Note<>(name, value, timestamp, false);
    }

    /**
     * Creates a new note that contains a string
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a String value
     */
    public static Note<List<String>> ofStrings(String name, List<String> value, long timestamp) {
        return new Note<>(name, value, timestamp, false);
    }

    /**
     * Creates a new note that contains a long
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a long value
     */
    public static Note<List<Long>> ofLongs(String name, List<Long> value, long timestamp) {
        return new Note<>(name, value, timestamp, false);
    }

    /**
     * Creates a new note that contains a boolean
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a boolean value
     */
    public static Note<List<Boolean>> ofBooleans(String name, List<Boolean> value, long timestamp) {
        return new Note<>(name, value, timestamp, false);
    }

    /**
     * Creates a new note that contains a double
     * @param name The name of the note
     * @param value The value for the note
     * @return A new {@link Note} that contains a double value
     */
    public static Note<List<Double>> ofDoubles(String name, List<Double> value, long timestamp) {
        return new Note<>(name, value, timestamp, false);
    }

    public static Note<?> fromAttributeValue(String name, AttributeValue attributeValue) {
        return fromAttributeValue(name, attributeValue, false, System.currentTimeMillis());
    }

    public static Note<?> fromAttributeValue(String name, AttributeValue attributeValue, boolean sticky) {
        return fromAttributeValue(name, attributeValue, sticky, System.currentTimeMillis());
    }

    public static Note<?> fromAttributeValue(String name, AttributeValue attributeValue, long timestamp) {
        return fromAttributeValue(name, attributeValue, false, timestamp);
    }

    public static Note<?> fromAttributeValue(String name, AttributeValue attributeValue, boolean sticky, long timestamp) {
        if (attributeValue.isNull()) {
            return of(name, null, sticky, timestamp);
        }
        switch (attributeValue.getType()) {
            case STRING:
                return of(name, attributeValue.getStringValue(), sticky, timestamp);
            case BOOLEAN:
                return of(name, attributeValue.getBooleanValue(), sticky, timestamp);
            case LONG:
                return of(name, attributeValue.getLongValue(), sticky, timestamp);
            case DOUBLE:
                return of(name, attributeValue.getDoubleValue(), sticky, timestamp);
            case STRING_ARRAY:
                return ofStrings(name, attributeValue.getStringArrayValue(), sticky, timestamp);
            case BOOLEAN_ARRAY:
                return ofBooleans(name, attributeValue.getBooleanArrayValue(), sticky, timestamp);
            case LONG_ARRAY:
                return ofLongs(name, attributeValue.getLongArrayValue(), sticky, timestamp);
            case DOUBLE_ARRAY:
                return ofDoubles(name, attributeValue.getDoubleArrayValue(), sticky, timestamp);
        }
        throw new IllegalArgumentException(String.format("attributeValue type %s is not an expected type.", attributeValue.getType()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Note<?> note = (Note<?>) o;

        if (timestamp != note.timestamp) return false;
        if (sticky != note.sticky) return false;
        if (!name.equals(note.name)) return false;
        return Objects.equals(value, note.value);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (sticky ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Note(");
        builder.append("name=").append(name);
        builder.append(", value=").append(value == null ? "null" : value.toString());
        builder.append(", timestamp=").append(timestamp);
        builder.append(", sticky=").append(sticky);
        builder.append(")");
        return builder.toString();
    }
}
