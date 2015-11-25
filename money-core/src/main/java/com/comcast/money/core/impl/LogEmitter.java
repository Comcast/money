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

import java.util.logging.Level;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comcast.money.core.SpanData;
import com.comcast.money.core.SpanEmitter;

public class LogEmitter implements SpanEmitter {

    private Logger logger;
    private Level logLevel;

    @Override
    public void emit(final SpanData spanData) {
        System.out.println("\r\n!!! logging data");
        System.out.println(spanData);
        if (logLevel == Level.SEVERE) {
            logger.error(spanData.toString());
        } else if (logLevel == Level.WARNING) {
            logger.warn(spanData.toString());
        } else if (logLevel == Level.INFO) {
            logger.info(spanData.toString());
        } else if (logLevel == Level.FINE) {
            logger.debug(spanData.toString());
        } else if (logLevel != Level.OFF) {
            logger.trace(spanData.toString());
        }
        // we do not log if the Level is set to OFF
    }

    @Override
    public void configure(Config emitterConf) {

        String level = emitterConf.getString("log-level");
        String name = emitterConf.getString("log-name");

        logLevel = Level.parse(level);
        logger = LoggerFactory.getLogger(name);
    }
}
