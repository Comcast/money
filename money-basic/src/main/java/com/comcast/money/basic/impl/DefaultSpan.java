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
    private final Long timeout;
    private final String spanName;
    private Long startTimeNano;
    private Long startTime = 0L;
    private Long stopTime;
    private Long duration = 0L;
    private boolean success = true;
    private Map<String, Long> timers = new HashMap<String, Long>();
    private Map<String, Note<?>> notes = new HashMap<String, Note<?>>();
    private boolean closed = false;

    public DefaultSpan(SpanId spanId, String spanName, SpanEmitter spanEmitter, Long timeout) {
        this.spanId = spanId;
        this.spanEmitter = spanEmitter;
        this.timeout = timeout;
        this.spanName = spanName;
    }

    @Override
    public void begin(Long startTime, Span parentSpan, boolean propagate) {
        this.startTime = startTime;
        this.startTimeNano = System.nanoTime() / 1000;

        if (propagate && parentSpan != null) {
            notes.putAll(parentSpan.data().getNotes());
        }
    }

    @Override
    public synchronized void end(Long endTime, boolean result) {
        // TODO: need to implement a timeout after getting an end signal, we need to go into a "Closing" state
        // and continue to allow all calls until we are done...
        // TODO: also, need to add (isOpen) checks to all calls; that said, we only emit once, and
        // anything that comes in after we are closed ultimately gets dropped on the floor
        this.stopTime = endTime;
        this.duration = (System.nanoTime() / 1000) - this.startTimeNano;
        success = result;
        if (!closed) {
            spanEmitter.emit(data());
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

    @Override
    public SpanData data() {
        return new SpanData(notes, startTime, stopTime, success, spanId, spanName, duration);
    }

    @Override
    public void timedOut() {

        if (!closed) {
            System.out.println("\r\n\r\n!!!TIMEOUT!!!");
            logger.warn("span {} timed out", spanName);
            end(System.currentTimeMillis(), false);
        }
    }

    @Override
    public boolean isExpired() {
        Long expireTime = startTime + timeout;
        System.out.println("checking if expired for span; expireTime = " + expireTime + "; now = " + System.currentTimeMillis());
        return (startTime + timeout) < System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultSpan that = (DefaultSpan) o;

        return spanId.equals(that.spanId);

    }

    @Override
    public int hashCode() {
        return spanId.hashCode();
    }
}
