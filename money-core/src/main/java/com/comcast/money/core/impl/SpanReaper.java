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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comcast.money.core.Span;

public class SpanReaper {

    private static final Logger logger = LoggerFactory.getLogger(SpanReaper.class);

    private final LinkedBlockingQueue<Span> activeSpans = new LinkedBlockingQueue<Span>();
    private final LinkedBlockingQueue<Span> closingSpans = new LinkedBlockingQueue<Span>();
    private final ScheduledExecutorService scheduler;
    private final long reaperInterval;

    public SpanReaper(ScheduledExecutorService executorService, long reaperInterval) {
        this.scheduler = executorService;
        this.reaperInterval = reaperInterval;

        // Schedules the cleanup of spans that expired
        this.scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                reap();
            }
        }, 100L, reaperInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Called, typically on a timer, to reap the activeSpans who have expired and
     * to close spans that are closing
     */
    public void reap() {

        try {
            System.out.println("...reaping...");
            // first pass is to finalize all activeSpans that should be closed
            // we start at the top of the queue since those are the oldest, if they are closed
            // then we can remove them from the queue
            while (!closingSpans.isEmpty() && closingSpans.peek().shouldClose()) {
                Span closedSpan = closingSpans.poll();
                closedSpan.close();
            }

            // because this is a LinkedBlockingQueue, we can assert that the queue is ordered FIFO,
            // so the oldest appear at the top of the queue...if the items at the top of the queue
            // are not expired, then it follows that those later down in the list are also not expired
            while (!activeSpans.isEmpty() && activeSpans.peek().isTimedOut()) {
                Span expired = activeSpans.poll();
                expired.timedOut();
            }
        } catch(Exception ex) {
            logger.error("encountered unexpected error in reap", ex);
        }
    }

    /**
     * Starts watching a span for timeout
     * @param span The Span to watch
     */
    public void watch(Span span) {
        activeSpans.offer(span);
    }

    /**
     * Stops watching a span for timeout, queues it for closing
     * @param span The Span to stop watching
     */
    public void unwatch(Span span) {

        // Remove the span from the active span list...
        if (!activeSpans.remove(span)) {
            logger.warn("attempt was made to unwatch a span; but the span was not found");
        }

        // Add the span to the closing spans list for cleanup on the next reap...
        System.out.println("...unwatching span " + span.data().getName());
        closingSpans.add(span);
    }
}
