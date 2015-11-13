package com.comcast.money.core;

public class Note<T> {

    private final String name;
    private final T value;
    private final Long timestamp;

    public Note(String name, T value) {
        this(name, value, System.currentTimeMillis());
    }

    public Note(String name, T value, Long timestamp) {
        this.name = name;
        this.value = value;
        this.timestamp = timestamp;
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
}
