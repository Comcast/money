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

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.typesafe.config.Config;

import com.comcast.money.core.SpanData;
import com.comcast.money.core.SpanEmitter;

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
