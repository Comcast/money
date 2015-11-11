package com.comcast.money.basic.impl;

import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

import org.slf4j.Logger;

import com.comcast.money.basic.SpanData;
import com.comcast.money.basic.SpanEmitter;

public class LogEmitter implements SpanEmitter {

    private final ExecutorService executorService;
    private final Logger logger;
    private final Level logLevel;

    public LogEmitter(ExecutorService executorService, Logger logger, Level logLevel) {
        this.executorService = executorService;
        this.logger = logger;
        this.logLevel = logLevel;
    }

    @Override
    public void emit(final SpanData spanData) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (logLevel == Level.SEVERE) {
                    logger.error(toLogEntry(spanData));
                } else if (logLevel == Level.WARNING) {
                    logger.warn(toLogEntry(spanData));
                } else if (logLevel == Level.INFO) {
                    logger.info(toLogEntry(spanData));
                } else if (logLevel == Level.FINE) {
                    logger.debug(toLogEntry(spanData));
                } else if (logLevel != Level.OFF) {
                    logger.trace(toLogEntry(spanData));
                }
                // we do not log if the Level is set to OFF
            }
        });
    }

    private String toLogEntry(SpanData spanData) {
        // TODO: implement log formatting
        return "";
    }
}
