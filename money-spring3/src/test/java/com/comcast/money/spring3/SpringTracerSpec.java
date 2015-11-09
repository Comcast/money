package com.comcast.money.spring3;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.comcast.money.core.Money$;
import com.comcast.money.core.Tracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SpringTracerSpec {

    @Mock
    private Tracer mockTracer;

    private SpringTracer underTest = new SpringTracer();

    @Before
    public void setUp() {
        underTest.setTracer(mockTracer);
    }

    @Test
    public void testSpringTracerUsesMoneySpanSupervisor() {

        // Test coverage...bonus!
        SpringTracer springTracer = new SpringTracer();
        assertThat(springTracer.spanSupervisorRef()).isSameAs(Money$.MODULE$.tracer().spanSupervisorRef());
    }

    @Test
    public void testSpringTracerDelegatesForClose() {
        underTest.close();
        verify(mockTracer).close();
    }

    @Test
    public void testSpringTracerDelegatesForRecordBoolean() {
        underTest.record("key", false);
        verify(mockTracer).record("key", false);
    }

    @Test
    public void testSpringTracerDelegatesForRecordBooleanWithPropagate() {
        underTest.record("key", true, true);
        verify(mockTracer).record("key", true, true);
    }

    @Test
    public void testSpringTracerDelegatesForRecordDouble() {
        underTest.record("dbl", 0.0);
        verify(mockTracer).record("dbl", 0.0);
    }

    @Test
    public void testSpringTracerDelegatesForRecordDoubleWithPropagate() {
        underTest.record("dbl", 0.0, true);
        verify(mockTracer).record("dbl", 0.0, true);
    }

    @Test
    public void testSpringTracerDelegatesForRecordLong() {
        underTest.record("lng", 1L);
        verify(mockTracer).record("lng", 1L);
    }

    @Test
    public void testSpringTracerDelegatesForRecordLongWithPropagate() {
        underTest.record("lng", 1L, true);
        verify(mockTracer).record("lng", 1L, true);
    }

    @Test
    public void testSpringTracerDelegatesForRecordString() {
        underTest.record("str", "foo");
        verify(mockTracer).record("str", "foo");
    }

    @Test
    public void testSpringTracerDelegatesForRecordStringWithPropagate() {
        underTest.record("str", "foo", true);
        verify(mockTracer).record("str", "foo", true);
    }

    @Test
    public void testSpringTracerDelegatesForStartSpan() {
        underTest.startSpan("newSpan");
        verify(mockTracer).startSpan("newSpan");
    }

    @Test
    public void testSpringTracerDelegatesForStopTimer() {
        underTest.stopTimer("timer");
        verify(mockTracer).stopTimer("timer");
    }

    @Test
    public void testSpringTracerDelegatesForStartTimer() {
        underTest.startTimer("timer");
        verify(mockTracer).startTimer("timer");
    }

    @Test
    public void testSpringTracerDelegatesForTime() {
        underTest.time("timer");
        verify(mockTracer).time("timer");
    }
}
