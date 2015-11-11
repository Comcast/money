package com.comcast.money.basic;

import java.util.Map;

public class SpanData {

    private final Map<String, Note<?>> notes;
    private final Long startTime;
    private final Long endTime;
    private final boolean success;
    private final SpanId spanId;

    public SpanData(Map<String, Note<?>> notes, Long startTime, Long endTime, boolean success, SpanId spanId) {
        this.notes = notes;
        this.startTime = startTime;
        this.endTime = endTime;
        this.success = success;
        this.spanId = spanId;
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

    public boolean isSuccess() {
        return success;
    }

    public SpanId getSpanId() {
        return spanId;
    }
}
