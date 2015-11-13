package com.comcast.money.core;

import com.typesafe.config.Config;

public interface SpanEmitter {

    /**
     * Callback to allow an emitter to configure itself
     * @param emitterConf
     */
    void configure(Config emitterConf);

    /**
     * Handles span data, this should be non-blocking
     * @param spanData representation of a span
     */
    void emit(SpanData spanData);
}
