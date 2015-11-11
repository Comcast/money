package com.comcast.money.basic.impl;

import java.util.List;

import com.comcast.money.basic.SpanData;
import com.comcast.money.basic.SpanEmitter;

/**
 * SpanEmitter that works as a router for a set of other span emitters
 */
public class SpanEmitterRouter implements SpanEmitter {

    private final List<SpanEmitter> emitters;

    public SpanEmitterRouter(List<SpanEmitter> emitters) {
        this.emitters = emitters;
    }

    @Override
    public void emit(SpanData spanData) {
        for(SpanEmitter emitter : emitters) {
            emitter.emit(spanData);
        }
    }
}
