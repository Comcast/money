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

package com.comcast.money.reflect;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.comcast.money.annotations.TracedData;
import com.comcast.money.core.Note;
import com.comcast.money.core.Tracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TracedDataParameterTest {

    @Mock
    private Tracer tracer;

    @Mock
    private TracedData tracedData;

    @Captor
    private ArgumentCaptor<Note<?>> noteCaptor;

    @Before
    public void setUp() {
        when(tracedData.value()).thenReturn("traced");
    }

    @Test
    public void testString() {

        TracedDataParameter<String> underTest = new TracedDataParameter<String>(tracedData, "foo", String.class);
        assertThat(underTest.getName()).isEqualTo("traced");
        assertThat(underTest.getParameterType()).isEqualTo(String.class);
        assertThat(underTest.getParameterValue()).isEqualTo("foo");

        underTest.trace(tracer);

        verify(tracer).record(noteCaptor.capture());
        Note<?> note = noteCaptor.getValue();

        assertThat(note.getName()).isEqualTo("traced");
        assertThat(note.getValue()).isEqualTo("foo");
    }

    @Test
    public void testNull() {

        String val = null;
        TracedDataParameter<String> underTest = new TracedDataParameter<String>(tracedData, val, String.class);
        assertThat(underTest.getName()).isEqualTo("traced");
        assertThat(underTest.getParameterType()).isEqualTo(String.class);
        assertThat(underTest.getParameterValue()).isNull();

        underTest.trace(tracer);

        verify(tracer).record(noteCaptor.capture());
        Note<?> note = noteCaptor.getValue();

        assertThat(note.getName()).isEqualTo("traced");
        assertThat(note.getValue()).isEqualTo(null);
    }
}
