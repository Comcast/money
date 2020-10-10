/*
 * Copyright 2012 Comcast Cable Communications Management, LLC
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

package com.comcast.money.spring;

import io.opentelemetry.context.Scope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.comcast.money.api.Note;
import com.comcast.money.api.Span;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-context.xml")
public class TracedMethodInterceptorSpec {

    @Autowired
    private SampleTraceBean sampleTraceBean;

    // This bean is intercepted by springockito, so it is actually a mock!  Living the life!
    @Autowired
    private SpringTracer springTracer;

    @Mock
    private Span.Builder spanBuilder;

    @Mock
    private Span span;

    @Mock
    private Scope scope;

    @Captor
    private ArgumentCaptor<Boolean> spanResultCaptor;

    @Before
    public void setUp() {
        // Needed to init the Argument Captor
        MockitoAnnotations.initMocks(this);

        when(springTracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(springTracer.withSpan(span)).thenReturn(scope);
    }

    @After
    public void tearDown() {
        // Reset the mocks so we can continue to do verifies across tests
        reset(springTracer);
    }

    @Test
    public void testTracing() throws Exception {

        sampleTraceBean.doSomethingGood();
        verify(springTracer).spanBuilder("SampleTrace");
        verify(spanBuilder).startSpan();
        verify(springTracer).withSpan(span);
        verify(springTracer).record("foo", "bar", false);
        verify(span).stop(true);
        verify(scope).close();
    }

    @Test
    public void testTracedDataParamsWithValues() throws Exception {

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);

        sampleTraceBean.doSomethingWithTracedParams("tp", true, 200L, 3.14);
        verify(spanBuilder, times(4)).record(noteCaptor.capture());

        Note<String> stringNote = (Note<String>)noteCaptor.getAllValues().get(0);
        assertThat(stringNote.name()).isEqualTo("STRING");
        assertThat(stringNote.value()).isEqualTo("tp");

        Note<Boolean> booleanNote = (Note<Boolean>)noteCaptor.getAllValues().get(1);
        assertThat(booleanNote.name()).isEqualTo("BOOLEAN");
        assertThat(booleanNote.value()).isEqualTo(true);

        Note<Long> longNote = (Note<Long>)noteCaptor.getAllValues().get(2);
        assertThat(longNote.name()).isEqualTo("LONG");
        assertThat(longNote.value()).isEqualTo(200L);

        Note<Double> doubleNote = (Note<Double>)noteCaptor.getAllValues().get(3);
        assertThat(doubleNote.name()).isEqualTo("DOUBLE");
        assertThat(doubleNote.value()).isEqualTo(3.14);
    }

    @Test
    public void testTracedDataParamsWithNullValues() throws Exception {

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);

        sampleTraceBean.doSomethingWithTracedParams(null, null, null, null);
        verify(spanBuilder, times(4)).record(noteCaptor.capture());

        Note<String> stringNote = (Note<String>)noteCaptor.getAllValues().get(0);
        assertThat(stringNote.name()).isEqualTo("STRING");
        assertThat(stringNote.value()).isNull();

        Note<String> booleanNote = (Note<String>)noteCaptor.getAllValues().get(1);
        assertThat(booleanNote.name()).isEqualTo("BOOLEAN");
        assertThat(booleanNote.value()).isNull();

        Note<String> longNote = (Note<String>)noteCaptor.getAllValues().get(2);
        assertThat(longNote.name()).isEqualTo("LONG");
        assertThat(longNote.value()).isNull();

        Note<String> doubleNote = (Note<String>)noteCaptor.getAllValues().get(3);
        assertThat(doubleNote.name()).isEqualTo("DOUBLE");
        assertThat(doubleNote.value()).isNull();
    }

    @Test
    public void testTracingRecordsFailureOnException() throws Exception {

        try {
            sampleTraceBean.doSomethingBad();
        }
        catch (Exception ex) {

        }
        verify(springTracer).spanBuilder("SampleTrace");
        verify(spanBuilder).startSpan();
        verify(springTracer).withSpan(span);
        verify(springTracer).record("foo", "bar", false);
        verify(span).stop(false);
        verify(scope).close();
    }

    @Test
    public void testTracingDoesNotTraceMethodsWithoutAnnotation() {

        sampleTraceBean.doSomethingNotTraced();
        verifyZeroInteractions(springTracer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTracingIgnoresException() {
        sampleTraceBean.doSomethingButIgnoreException();
        verify(span).stop(true);
    }
}
