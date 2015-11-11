package com.comcast.money.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comcast.money.basic.impl.DefaultSpanService;
import com.comcast.money.basic.impl.DefaultTracer;
import com.comcast.money.basic.impl.LogEmitter;
import com.comcast.money.basic.impl.SpanEmitterRouter;
import com.comcast.money.basic.impl.ThreadLocalTraceContext;

public class Money {

    public static Tracer tracer;
    private static ExecutorService moneyExecutor = Executors.newFixedThreadPool(50); // TODO: configurable
    private static ScheduledExecutorService moneyScheduler = Executors.newScheduledThreadPool(2);

    static {
        Logger logger = LoggerFactory.getLogger("money");
        LogEmitter logEmitter = new LogEmitter(moneyExecutor, logger, Level.INFO);

        List<SpanEmitter> emitters = new ArrayList<SpanEmitter>();
        emitters.add(logEmitter);

        SpanEmitterRouter router = new SpanEmitterRouter(emitters);

        SpanService spanService = new DefaultSpanService(moneyExecutor, router, moneyScheduler);
        tracer = new DefaultTracer(new ThreadLocalTraceContext(), spanService);
    }
}
