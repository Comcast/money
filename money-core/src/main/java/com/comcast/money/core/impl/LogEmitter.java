package com.comcast.money.core.impl;

import java.util.logging.Level;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comcast.money.core.Note;
import com.comcast.money.core.SpanData;
import com.comcast.money.core.SpanEmitter;
import com.comcast.money.core.SpanId;

public class LogEmitter implements SpanEmitter {

    private static final String HEADER_FORMAT = "Span: [ span-id=%s ][ trace-id=%s ][ parent-id=%s ][ span-name=%s ][ app-name=%s ][ start-time=%s ][ span-duration=%s ][ span-success=%s ]";
    private static final String NOTE_FORMAT = "[ %s=%s ]";
    private static final String NULL = "NULL";

    private Logger logger;
    private Level logLevel;

    @Override
    public void emit(final SpanData spanData) {
        System.out.println("\r\n!!! logging data");
        System.out.println(toLogEntry(spanData));
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

    @Override
    public void configure(Config emitterConf) {

        String level = emitterConf.getString("log-level");
        String name = emitterConf.getString("log-name");

        logLevel = Level.parse(level);
        logger = LoggerFactory.getLogger(name);
    }

    private String toLogEntry(SpanData spanData) {
        SpanId id = spanData.getSpanId();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(HEADER_FORMAT, id.getSelfId(), id.getTraceId(), id.getParentId(), spanData.getName(), "app", spanData.getStartTime(), spanData.getDuration(), spanData.isSuccess()));

        if (spanData.getNotes() != null) {
            for(Note<?> note : spanData.getNotes().values()) {
                sb.append(String.format(NOTE_FORMAT, note.getName(), valueOrNull(note.getValue())));
            }
        }

        return sb.toString();
    }

    private Object valueOrNull(Object value) {
        if (value == null) {
            return NULL;
        } else {
            return value;
        }
    }
}
