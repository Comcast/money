package com.comcast.money.api;

public class Note<T> {

    private final String name;
    private final T value;
    private final Long timestamp;
    private final boolean propagated;

    private Note(String name, T value) {
        this(name, value, System.currentTimeMillis(), false);
    }

    private Note(String name, T value, boolean propagated) {
        this(name, value, System.currentTimeMillis(), propagated);
    }

    private Note(String name, T value, Long timestamp, boolean propagated) {
        this.name = name;
        this.value = value;
        this.timestamp = timestamp;
        this.propagated = propagated;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public boolean isPropagated() {
        return propagated;
    }

    public static Note<String> of(String name, String value) {
        return new Note<String>(name, value);
    }

    public static Note<Long> of(String name, Long value) {
        return new Note<Long>(name, value);
    }

    public static Note<Boolean> of(String name, Boolean value) {
        return new Note<Boolean>(name, value);
    }

    public static Note<Double> of(String name, Double value) {
        return new Note<Double>(name, value);
    }

    public static Note<String> of(String name, String value, boolean propagated) {
        return new Note<String>(name, value, propagated);
    }

    public static Note<Long> of(String name, Long value, boolean propagated) {
        return new Note<Long>(name, value, propagated);
    }

    public static Note<Boolean> of(String name, Boolean value, boolean propagated) {
        return new Note<Boolean>(name, value, propagated);
    }

    public static Note<Double> of(String name, Double value, boolean propagated) {
        return new Note<Double>(name, value, propagated);
    }
}