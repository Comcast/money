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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.comcast.money.annotations.Traced;
import com.comcast.money.core.Tracer;
import com.comcast.money.reflect.ReflectionUtils;
import com.comcast.money.reflect.TracedDataParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TracedMethodInterceptorTest {

    @Mock
    private SpringTracer tracer;

    @Mock
    private ReflectionUtils reflectionUtils;

    @Mock
    private MethodInvocation methodInvocation;

    @Mock
    private Traced traced;

    @Mock
    private TracedDataParameter<String> tracedDataParameter;

    @Mock
    private AccessibleObject staticPart;

    private TracedMethodInterceptor underTest;

    private Method methodWithoutArguments;

    private List<TracedDataParameter> tracedDataParameters = new ArrayList<TracedDataParameter>();

    @Before
    public void setUp() throws Throwable {
        underTest = new TracedMethodInterceptor(tracer, reflectionUtils);

        methodWithoutArguments = this.getClass().getMethod("methodWithoutArguments");
        tracedDataParameters.add(tracedDataParameter);

        when(methodInvocation.getStaticPart()).thenReturn(staticPart);
        when(staticPart.getAnnotation(Traced.class)).thenReturn(traced);
        when(traced.value()).thenReturn("methodWithoutArguments");
        when(methodInvocation.getMethod()).thenReturn(methodWithoutArguments);
        when(methodInvocation.getArguments()).thenReturn(new Object[0]);
        when(reflectionUtils.extractTracedParameters(methodWithoutArguments, new Object[0])).thenReturn(tracedDataParameters);

        when(methodInvocation.proceed()).thenReturn("foo");
    }

    @Test
    public void testInvoke() throws Throwable {

        Object result = underTest.invoke(methodInvocation);
        assertThat(result).isEqualTo("foo");

        verify(tracer).startSpan("methodWithoutArguments");
        verify(tracedDataParameter).trace(tracer);
        verify(tracer).stopSpan(true);
    }

    @Test
    public void testInvokeWithoutTracedAnnotation() throws Throwable {

        when(staticPart.getAnnotation(Traced.class)).thenReturn(null);

        Object result = underTest.invoke(methodInvocation);
        assertThat(result).isEqualTo("foo");

        verifyZeroInteractions(tracer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvokeWithException() throws Throwable {

        when(methodInvocation.proceed()).thenThrow(new IllegalArgumentException("fail"));

        try {
            underTest.invoke(methodInvocation);
        } finally {
            verify(tracer).startSpan("methodWithoutArguments");
            verify(tracedDataParameter).trace(tracer);
            verify(tracer).stopSpan(false);
        }
    }

    @Traced("methodWithoutArguments")
    public void methodWithoutArguments() throws Throwable {
    }
}
