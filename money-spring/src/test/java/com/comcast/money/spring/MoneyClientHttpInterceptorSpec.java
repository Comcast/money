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

import com.comcast.money.api.InstrumentationLibrary;
import com.comcast.money.api.Span;
import com.comcast.money.api.SpanId;
import com.comcast.money.api.SpanInfo;
import com.comcast.money.core.CoreSpanInfo;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.api.trace.StatusCode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;

import java.util.Collections;

import static org.mockito.Mockito.*;

public class MoneyClientHttpInterceptorSpec {

    private final static SpanId id = SpanId.createNew().createChild();
    private final static String traceId = id.traceId();
    private final static long spanId = id.selfId();
    private final static long parentSpanId = id.parentId();

    private Scope spanScope;

    @Before
    public void setUp() {
        Span span = mock(Span.class);
        SpanInfo testSpanInfo = new CoreSpanInfo(
                id,
                "testName",
                SpanKind.INTERNAL,
                true,
                0L,
                0L,
                true,
                0L,
                StatusCode.OK,
                "",
                Collections.emptyMap(),
                Collections.emptyList(),
                Collections.emptyList(),
                new InstrumentationLibrary("test", "0.0.1"),
                "testAppName",
                "testHost");

        when(span.info()).thenReturn(testSpanInfo);
        when(span.storeInContext(any())).thenCallRealMethod();

        Context updatedContext = Context.root().with(span);
        spanScope = updatedContext.makeCurrent();
    }

    @After
    public void tearDown() throws Exception {
        spanScope.close();
    }

    @Test
    public void testMoneyAndTraceParentHeadersAreSet() throws Exception {
        System.out.printf("Trace ID: %s%nSpan ID: %d%nParent ID: %d%n", traceId, spanId, parentSpanId);

        HttpRequest httpRequest = mock(HttpRequest.class);
        ClientHttpRequestExecution clientHttpRequestExecution = mock(ClientHttpRequestExecution.class);
        MoneyClientHttpRequestInterceptor underTest = new MoneyClientHttpRequestInterceptor();

        HttpHeaders httpHeaders = new HttpHeaders();
        when(httpRequest.getHeaders()).thenReturn(httpHeaders);

        String expectedMoneyHeaderVal = String.format("trace-id=%s;parent-id=%s;span-id=%s", traceId, parentSpanId, spanId);
        String expectedTraceParentHeaderVal = String.format("00-%s-%016x-01", traceId.replace("-", ""), spanId);

        underTest.intercept(httpRequest, new byte[0], clientHttpRequestExecution);

        verify(httpRequest).getHeaders();
        Assert.assertEquals(2, httpHeaders.size());
        Assert.assertEquals(expectedMoneyHeaderVal, httpHeaders.get("X-MoneyTrace").get(0));
        Assert.assertEquals(expectedTraceParentHeaderVal, httpHeaders.get("traceparent").get(0));
    }
}

