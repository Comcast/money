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

package com.comcast.money.core.impl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comcast.money.core.Note;
import com.comcast.money.core.Span;
import com.comcast.money.core.SpanData;
import com.comcast.money.core.SpanEmitter;
import com.comcast.money.core.SpanId;

import static com.comcast.money.core.TimeUtils.thisInstant;

public class DefaultSpan implements Span {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSpan.class);

    private enum State {
        Initial, Open, Stopped, Closed
    }

    private final SpanId spanId;
    private final SpanEmitter spanEmitter;
    private final Long timeout;
    private final String spanName;
    private final Long closeDelay;

    private Long startInstant;
    private Long startTime = 0L;
    private Long stoppedTime;
    private Long duration = 0L;
    private boolean success = true;
    private Map<String, Long> timers = new HashMap<String, Long>();
    private Map<String, Note<?>> notes = new HashMap<String, Note<?>>();
    private State state = State.Initial;

    public DefaultSpan(SpanId spanId, String spanName, SpanEmitter spanEmitter, Long timeout, Long closeDelay) {
        this.spanId = spanId;
        this.spanEmitter = spanEmitter;
        this.timeout = timeout;
        this.spanName = spanName;
        this.closeDelay = closeDelay;
    }

    public DefaultSpan(SpanId spanId, String spanName, SpanEmitter spanEmitter, Long timeout, Long closeDelay, SpanData parentData) {
        this(spanId, spanName, spanEmitter, timeout, closeDelay);
        if (parentData != null) {
            notes.putAll(parentData.getNotes());
        }
    }

    @Override
    public void start() {
        this.startTime = System.currentTimeMillis();
        this.startInstant = thisInstant();
        this.state = State.Open;
    }

    @Override
    public synchronized void stop(boolean result) {
        System.out.println("...stop()..." + state);
        stoppedTime = System.currentTimeMillis();
        duration = thisInstant() - startInstant;
        success = result;
        state = State.Stopped;
    }

    @Override
    public synchronized void record(Note<?> note) {
        notes.put(note.getName(), note);
    }

    @Override
    public synchronized void startTimer(String timerKey) {
        timers.put(timerKey, thisInstant());
    }

    @Override
    public synchronized void stopTimer(String timerKey) {
        Long startTime = timers.remove(timerKey);
        Long duration = 0L;
        if (startTime != null) {
            duration = startTime - thisInstant();
        }
        notes.put(timerKey, new Note<Long>(timerKey, duration));
    }

    @Override
    public SpanData data() {
        return new SpanData(notes, startTime, stoppedTime, success, spanId, spanName, duration);
    }

    @Override
    public void timedOut() {

        if (state != State.Closed) {
            System.out.println("\r\n\r\n!!!TIMEOUT!!!");
            logger.warn("span {} timed out", spanName);

            // stop then immediately close
            stop(false);
            close();
        }
    }

    @Override
    public boolean isTimedOut() {
        Long expireTime = startTime + timeout;
        System.out.println("checking if timed out for span " + spanName + "; expireTime = " + expireTime + "; now = " + System.currentTimeMillis());
        return System.currentTimeMillis() > expireTime;
    }

    @Override
    public void close() {
        System.out.println("...close()..." + state);
        if (state != State.Closed) {
            spanEmitter.emit(data());
            state = State.Closed;
        } else {
            logger.warn("Received end request to span that was already closed, spanId='{}'", spanId);
        }
    }

    @Override
    public boolean shouldClose() {
        // we should close if the time we have been stopped exceeds the close delay
        long stoppageTime = System.currentTimeMillis() - stoppedTime;
        System.out.println("shouldClose...stoppageTime = " + stoppageTime + "; closeDelay = " + closeDelay);
        return state == State.Closed || (stoppageTime > closeDelay);
    }

    @Override
    public Span newChild(String childName, boolean propagate) {
        SpanId childId = spanId.newChild();
        Span child;

        if (propagate) {
            child = new DefaultSpan(childId, childName, spanEmitter, timeout, closeDelay, data());
        } else {
            child = new DefaultSpan(childId, childName, spanEmitter, timeout, closeDelay);
        }

        return child;
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
