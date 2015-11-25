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

package com.comcast.money.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.typesafe.config.Config;

import com.comcast.money.core.impl.SpanEmitterRouter;

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
        System.out.println("\r\n\r\n!!!loading emitter " + className);
        Object emitter = Class.forName(className).newInstance();
        SpanEmitter spanEmitter = (SpanEmitter)emitter;
        spanEmitter.configure(emitterConf);

        return spanEmitter;
    }
}
