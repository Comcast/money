package com.comcast.money.spring3;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comcast.money.annotations.Traced;
import com.comcast.money.annotations.TracedData;

@Component
public class SampleTraceBean {

    @Autowired
    private SpringTracer springTracer;

    @Traced("SampleTrace")
    public void doSomethingGood() {

        springTracer.record("foo", "bar", false);
    }

    @Traced("SampleTrace")
    public void doSomethingBad() {

        springTracer.record("foo", "bar", false);
        throw new IllegalStateException("fail");
    }

    @Traced("SampleTrace")
    public void doSomethingWithTracedParams(
            @TracedData("STRING") String str,
            @TracedData("BOOLEAN") Boolean bool,
            @TracedData("LONG") Long lng,
            @TracedData("DOUBLE") Double dbl) {


    }

    public void doSomethingNotTraced() {
    }
}
