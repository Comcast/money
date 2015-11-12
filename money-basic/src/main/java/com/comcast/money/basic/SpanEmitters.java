package com.comcast.money.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.typesafe.config.Config;

import com.comcast.money.basic.impl.SpanEmitterRouter;

public class SpanEmitters {

    // TODO: this is fugly, need a better way to get the executor service to the span router!
    public static SpanEmitter load(Config config, ExecutorService executorService) {
        if (config.hasPath("money.emitters")) {
            try {
                return loadRouter(config, executorService);
            } catch(Exception ex) {
                throw new RuntimeException("Error loading emitters", ex);
            }
        } else {
            return new SpanEmitter() {
                @Override
                public void configure(Config emitterConf) {

                }
                @Override
                public void emit(SpanData spanData) {
                    // NO-OP
                }
            };
        }
    }

    private static SpanEmitterRouter loadRouter(Config config, ExecutorService executorService) throws Exception {
        List<SpanEmitter> emitters = new ArrayList<SpanEmitter>();

        for(Config emitterConfig : config.getConfigList("money.emitters")) {
            SpanEmitter emitter = loadEmitter(emitterConfig);
            emitters.add(emitter);
        }
        return new SpanEmitterRouter(emitters, executorService);
    }

    private static SpanEmitter loadEmitter(Config emitterConf) throws Exception {
        String className = emitterConf.getString("class-name");
        Object emitter = Class.forName(className).newInstance();
        SpanEmitter spanEmitter = (SpanEmitter)emitter;
        spanEmitter.configure(emitterConf);

        return spanEmitter;
    }
}
