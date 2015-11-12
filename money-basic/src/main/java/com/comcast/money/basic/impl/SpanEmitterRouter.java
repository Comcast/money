package com.comcast.money.basic.impl;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.typesafe.config.Config;

import com.comcast.money.basic.SpanData;
import com.comcast.money.basic.SpanEmitter;

/**
 * SpanEmitter that works as a router for a set of other span emitters
 */
public class SpanEmitterRouter implements SpanEmitter {

    private final List<SpanEmitter> emitters;
    private final ExecutorService executorService;

    public SpanEmitterRouter(List<SpanEmitter> emitters, ExecutorService executorService) {
        this.emitters = emitters;
        this.executorService = executorService;
    }

    @Override
    public void emit(final SpanData spanData) {
        for(final SpanEmitter emitter : emitters) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    emitter.emit(spanData);
                }
            });
        }
    }

    @Override
    public void configure(Config emitterConf) {
        // TODO: we can probably build the emitter list here
    }
}
