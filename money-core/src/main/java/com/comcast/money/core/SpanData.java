package com.comcast.money.core;

import java.util.Map;

public class SpanData {

    private final Map<String, Note<?>> notes;
    private final Long startTime;
    private final Long endTime;
    private final Boolean success;
    private final SpanId spanId;
    private final String name;
    private final Long duration;

    public SpanData(Map<String, Note<?>> notes, Long startTime, Long endTime, boolean success, SpanId spanId, String name, Long duration) {
        this.notes = notes;
        this.startTime = startTime;
        this.endTime = endTime;
        this.success = success;
        this.spanId = spanId;
        this.name = name;
        this.duration = duration;
    }

    public Map<String, Note<?>> getNotes() {
        return notes;
    }

    public Long getStartTime() {
        return startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public Boolean isSuccess() {
        return success;
    }

    public SpanId getSpanId() {
        return spanId;
    }

    public String getName() {
        return name;
    }

    public Long getDuration() {
        return duration;
    }
}
