package com.comcast.money.api;

import java.util.Map;

public class SpanData {

    private static final String HEADER_FORMAT = "Span: [ span-id=%s ][ trace-id=%s ][ parent-id=%s ][ span-name=%s ][ app-name=%s ][ start-time=%s ][ span-duration=%s ][ span-success=%s ]";
    private static final String NOTE_FORMAT = "[ %s=%s ]";
    private static final String NULL = "NULL";

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(HEADER_FORMAT, spanId.getSelfId(), spanId.getTraceId(), spanId.getParentId(), name,
                "app", startTime, duration, success));

        if (notes != null) {
            for (Note<?> note : notes.values()) {
                sb.append(String.format(NOTE_FORMAT, note.getName(), valueOrNull(note.getValue())));
            }
        }

        return sb.toString();
    }

    private Object valueOrNull(Object value) {
        if (value == null) {
            return NULL;
        } else {
            return value;
        }
    }
}
