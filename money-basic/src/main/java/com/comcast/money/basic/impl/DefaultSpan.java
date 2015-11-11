package com.comcast.money.basic.impl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comcast.money.basic.Note;
import com.comcast.money.basic.Span;
import com.comcast.money.basic.SpanData;
import com.comcast.money.basic.SpanEmitter;
import com.comcast.money.basic.SpanId;

public class DefaultSpan implements Span {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSpan.class);
    private final SpanId spanId;
    private final SpanEmitter spanEmitter;
    private Long startTime;
    private Long stopTime;
    private boolean success = true;
    private Map<String, Long> timers = new HashMap<String, Long>();
    private Map<String, Note<?>> notes = new HashMap<String, Note<?>>();
    private boolean closed = false;

    public DefaultSpan(SpanId spanId, SpanEmitter spanEmitter) {
        this.spanId = spanId;
        this.spanEmitter = spanEmitter;
    }

    @Override
    public void begin(Long startTime) {
        this.startTime = startTime;
    }

    @Override
    public synchronized void end(Long endTime, boolean result) {
        // TODO: need to implement a timeout after getting an end signal, we need to go into a "Closing" state
        // and continue to allow all calls until we are done...
        // TODO: also, need to add (isOpen) checks to all calls; that said, we only emit once, and
        // anything that comes in after we are closed ultimately gets dropped on the floor
        this.stopTime = endTime;
        success = result;
        if (!closed) {
            spanEmitter.emit(new SpanData(notes, startTime, stopTime, success, spanId));
            closed = true;
        } else {
            logger.warn("Received end request to span that was already closed, spanId='{}'", spanId);
        }
    }

    @Override
    public synchronized void record(Note<?> note) {
        notes.put(note.getName(), note);
    }

    @Override
    public synchronized void startTimer(String timerKey, Long startTime) {
        timers.put(timerKey, startTime);
    }

    @Override
    public synchronized void stopTimer(String timerKey, Long endTime) {
        Long startTime = timers.remove(timerKey);
        Long duration = 0L;
        if (startTime != null) {
            duration = startTime - endTime;
        }
        notes.put(timerKey, new Note<Long>(timerKey, duration));
    }
}
