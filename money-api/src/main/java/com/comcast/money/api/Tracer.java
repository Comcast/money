package com.comcast.money.api;

public interface Tracer {

    Span newSpan(String spanName);

    Span newSpan(String spanName, boolean propagate);
}
