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

package com.comcast.money.aspectj;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.comcast.money.annotations.Traced;
import com.comcast.money.annotations.TracedData;
import com.comcast.money.core.Tracer;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TraceAspectTest {

    @InjectMocks
    private TraceAspect underTest;

    @Mock
    private ReflectionUtils reflectionUtils;

    @Mock
    private Tracer tracer;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private JoinPoint.StaticPart staticPart;

    @Mock
    private MethodSignature signature;

    @Mock
    private Traced traced;

    @Mock
    private TracedData tracedData;

    @Mock
    private TracedDataParameter tracedDataParameter;

    @Before
    public void setUp() throws Exception {
        when(proceedingJoinPoint.getStaticPart()).thenReturn(staticPart);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);
        when(staticPart.getSignature()).thenReturn(signature);
        when(traced.value()).thenReturn("trace-test");

        Method method = this.getClass().getMethod("sampleMethod");
        when(signature.getMethod()).thenReturn(method);

        when(tracedData.value()).thenReturn("trace-param");
        when(tracedData.propagate()).thenReturn(false);
    }

    @Test
    public void testMethodWithNoArguments() throws Throwable {

        underTest.adviseMethodsWithTracing(proceedingJoinPoint, traced);

        verify(tracer).startSpan("trace-test");
        verify(tracer).stopSpan(true);
    }

    @Test
    public void testMethodWithTracedDataArguments() throws Throwable {

        List<TracedDataParameter> params = new ArrayList<TracedDataParameter>();
        params.add(tracedDataParameter);

        when(reflectionUtils.extractTracedParameters(any(Method.class), any(Object[].class))).thenReturn(params);

        underTest.adviseMethodsWithTracing(proceedingJoinPoint, traced);
        verify(tracedDataParameter).trace(tracer);
    }

    @Test
    public void testNonMethodSignature() throws Throwable {

        Signature nonMethodSignature = mock(Signature.class);
        when(staticPart.getSignature()).thenReturn(nonMethodSignature);

        underTest.adviseMethodsWithTracing(proceedingJoinPoint, traced);
        verifyZeroInteractions(reflectionUtils);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testMethodThrowingException() throws Throwable {

        when(proceedingJoinPoint.proceed()).thenThrow(new IllegalArgumentException("fail"));

        try {
            underTest.adviseMethodsWithTracing(proceedingJoinPoint, traced);
        } catch(IllegalAccessException ex) {

            verify(tracer).stopSpan(false);
            throw ex;
        }
    }

    @Test
    public void testTracedPointcutDoesNothing() {

        underTest.traced(traced);
    }

    // needed because Method is final and we cannot mock reflection Method
    public void sampleMethod() {

    }
}
