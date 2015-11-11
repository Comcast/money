package com.comcast.money.basic.impl;

import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comcast.money.basic.Span;

public class SpanReaper {

    private static final Logger logger = LoggerFactory.getLogger(SpanReaper.class);

    private final LinkedBlockingQueue<Span> spans = new LinkedBlockingQueue<Span>();

    public SpanReaper() {

    }

    /**
     * Called, typically on a timer, to reap the spans who have expired
     */
    public void reap() {

        // because this is a LinkedBlockingQueue, we can assert that the queue is ordered FIFO,
        // so the oldest appear at the top of the queue...if the items at the top of the queue
        // are not expired, then it follows that those later down in the list are also not expired
        while(!spans.isEmpty() && spans.peek().isExpired()) {
            Span expired = spans.poll();
            expired.timedOut();
        }
    }

    public void watch(Span span) {
        spans.offer(span);
    }

    // TODO: QUESTION - what are the pros/cons to unwatching vs not immediately removing and waiting for reap to clear out the queue
    // the proposal would be to have the "end" call simple mark the span as "Closing", and it would go into a "Closed"
    // state once we are <reaper interval> millis past the last message that was received...WHY?...this would
    // give us the ability to hang around for a short time prior to emitting; which is how spans act today.
    public void unwatch(Span span) {
        if (!spans.remove(span)) {
            logger.warn("attempt was made to unwatch a span; but the span was not found");
        }
    }
}
