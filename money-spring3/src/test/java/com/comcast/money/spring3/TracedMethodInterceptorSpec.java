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

package com.comcast.money.spring3;

import com.comcast.money.api.Tag;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-context.xml")
public class TracedMethodInterceptorSpec {

    @Autowired
    private SampleTraceBean sampleTraceBean;

    // This bean is intercepted by springockito, so it is actually a mock!  Living the life!
    @Autowired
    private SpringTracer springTracer;

    @Captor
    private ArgumentCaptor<Boolean> spanResultCaptor;

    @Before
    public void setUp() {
        // Needed to init the Argument Captor
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        // Reset the mocks so we can continue to do verifies across tests
        reset(springTracer);
    }

    @Test
    public void testTracing() throws Exception {

        sampleTraceBean.doSomethingGood();
        verify(springTracer).startSpan("SampleTrace");
        verify(springTracer).record("foo", "bar", false);
        verifySpanResultsIn(true);
    }

    @Test
    public void testTracedDataParamsWithValues() throws Exception {

        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);

        sampleTraceBean.doSomethingWithTracedParams("tp", true, 200L, 3.14);
        verify(springTracer, times(4)).record(tagCaptor.capture());

        Tag<String> stringTag = (Tag<String>)tagCaptor.getAllValues().get(0);
        assertThat(stringTag.name()).isEqualTo("STRING");
        assertThat(stringTag.value()).isEqualTo("tp");

        Tag<Boolean> booleanTag = (Tag<Boolean>)tagCaptor.getAllValues().get(1);
        assertThat(booleanTag.name()).isEqualTo("BOOLEAN");
        assertThat(booleanTag.value()).isEqualTo(true);

        Tag<Long> longTag = (Tag<Long>)tagCaptor.getAllValues().get(2);
        assertThat(longTag.name()).isEqualTo("LONG");
        assertThat(longTag.value()).isEqualTo(200L);

        Tag<Double> doubleTag = (Tag<Double>)tagCaptor.getAllValues().get(3);
        assertThat(doubleTag.name()).isEqualTo("DOUBLE");
        assertThat(doubleTag.value()).isEqualTo(3.14);
    }

    @Test
    public void testTracedDataParamsWithNullValues() throws Exception {

        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);

        sampleTraceBean.doSomethingWithTracedParams(null, null, null, null);
        verify(springTracer, times(4)).record(tagCaptor.capture());

        Tag<String> stringTag = (Tag<String>)tagCaptor.getAllValues().get(0);
        assertThat(stringTag.name()).isEqualTo("STRING");
        assertThat(stringTag.value()).isNull();

        Tag<String> booleanTag = (Tag<String>)tagCaptor.getAllValues().get(1);
        assertThat(booleanTag.name()).isEqualTo("BOOLEAN");
        assertThat(booleanTag.value()).isNull();

        Tag<String> longTag = (Tag<String>)tagCaptor.getAllValues().get(2);
        assertThat(longTag.name()).isEqualTo("LONG");
        assertThat(longTag.value()).isNull();

        Tag<String> doubleTag = (Tag<String>)tagCaptor.getAllValues().get(3);
        assertThat(doubleTag.name()).isEqualTo("DOUBLE");
        assertThat(doubleTag.value()).isNull();
    }

    @Test
    public void testTracingRecordsFailureOnException() throws Exception {

        try {
            sampleTraceBean.doSomethingBad();
        }
        catch (Exception ex) {

        }
        verify(springTracer).startSpan("SampleTrace");
        verify(springTracer).record("foo", "bar", false);
        verifySpanResultsIn(false);
    }

    @Test
    public void testTracingDoesNotTraceMethodsWithoutAnnotation() {

        sampleTraceBean.doSomethingNotTraced();
        verifyZeroInteractions(springTracer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTracingIgnoresException() {
        sampleTraceBean.doSomethingButIgnoreException();
        verifySpanResultsIn(true);
    }

    private void verifySpanResultsIn(Boolean result) {

        verify(springTracer).stopSpan(spanResultCaptor.capture());
        Boolean spanResult = spanResultCaptor.getValue();
        assertThat(spanResult).isEqualTo(result);
    }
}
