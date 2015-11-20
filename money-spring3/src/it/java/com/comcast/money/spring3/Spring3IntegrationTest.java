package com.comcast.money.spring3;

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

import com.comcast.money.core.Note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-context.xml")
public class Spring3IntegrationTest {

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

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);

        sampleTraceBean.doSomethingWithTracedParams("tp", true, 200L, 3.14);
        verify(springTracer, times(4)).record(noteCaptor.capture());

        Note<String> stringNote = (Note<String>)noteCaptor.getAllValues().get(0);
        assertThat(stringNote.getName()).isEqualTo("STRING");
        assertThat(stringNote.getValue()).isEqualTo("tp");

        Note<Boolean> booleanNote = (Note<Boolean>)noteCaptor.getAllValues().get(1);
        assertThat(booleanNote.getName()).isEqualTo("BOOLEAN");
        assertThat(booleanNote.getValue()).isEqualTo(true);

        Note<Long> longNote = (Note<Long>)noteCaptor.getAllValues().get(2);
        assertThat(longNote.getName()).isEqualTo("LONG");
        assertThat(longNote.getValue()).isEqualTo(200L);

        Note<Double> doubleNote = (Note<Double>)noteCaptor.getAllValues().get(3);
        assertThat(doubleNote.getName()).isEqualTo("DOUBLE");
        assertThat(doubleNote.getValue()).isEqualTo(3.14);
    }

    @Test
    public void testTracedDataParamsWithNullValues() throws Exception {

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);

        sampleTraceBean.doSomethingWithTracedParams(null, null, null, null);
        verify(springTracer, times(4)).record(noteCaptor.capture());

        Note<String> stringNote = (Note<String>)noteCaptor.getAllValues().get(0);
        assertThat(stringNote.getName()).isEqualTo("STRING");
        assertThat(stringNote.getValue()).isEqualTo(null);

        Note<Boolean> booleanNote = (Note<Boolean>)noteCaptor.getAllValues().get(1);
        assertThat(booleanNote.getName()).isEqualTo("BOOLEAN");
        assertThat(booleanNote.getValue()).isEqualTo(null);

        Note<Long> longNote = (Note<Long>)noteCaptor.getAllValues().get(2);
        assertThat(longNote.getName()).isEqualTo("LONG");
        assertThat(longNote.getValue()).isEqualTo(null);

        Note<Double> doubleNote = (Note<Double>)noteCaptor.getAllValues().get(3);
        assertThat(doubleNote.getName()).isEqualTo("DOUBLE");
        assertThat(doubleNote.getValue()).isEqualTo(null);
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

    private void verifySpanResultsIn(Boolean result) {

        verify(springTracer).stopSpan(spanResultCaptor.capture());
        boolean spanResult = spanResultCaptor.getValue();
        assertThat(spanResult).isEqualTo(result);
    }
}

