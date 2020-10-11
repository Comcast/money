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

import com.comcast.money.api.Span;
import com.comcast.money.api.SpanId;
import com.comcast.money.api.SpanInfo;
import com.comcast.money.core.CoreSpanInfo;
import com.comcast.money.core.internal.SpanLocal;

import io.opentelemetry.context.Scope;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class MoneyClientHttpInterceptorSpec {

    private final static String traceId = UUID.randomUUID().toString();
    private final static long spanId = RandomUtils.nextLong();
    private final static long parentSpanId = RandomUtils.nextLong();

    private Scope spanScope;

    @Before
    public void setUp() {
        Span span = mock(Span.class);
        SpanInfo testSpanInfo = new CoreSpanInfo(
                new SpanId(traceId, parentSpanId, spanId),
                "testName",
                io.opentelemetry.trace.Span.Kind.INTERNAL,
                0L,
                0L,
                0L,
                0L,
                0L,
                Boolean.TRUE,
                "",
                Collections.emptyMap(),
                Collections.emptyList(),
                "testAppName",
                "testHost");

        when(span.info()).thenReturn(testSpanInfo);
        spanScope = SpanLocal.push(span);
    }

    @After
    public void tearDown() throws Exception {
        spanScope.close();
    }

    @Test
    public void testMoneyB3AndTraceParentHeadersAreSet() throws Exception {
        System.out.printf("Trace ID: %s%nSpan ID: %d%nParent ID: %d%n", traceId, spanId, parentSpanId);

        HttpRequest httpRequest = mock(HttpRequest.class);
        ClientHttpRequestExecution clientHttpRequestExecution = mock(ClientHttpRequestExecution.class);
        MoneyClientHttpRequestInterceptor underTest = new MoneyClientHttpRequestInterceptor();

        HttpHeaders httpHeaders = new HttpHeaders();
        when(httpRequest.getHeaders()).thenReturn(httpHeaders);

        String expectedMoneyHeaderVal = String.format("trace-id=%s;parent-id=%s;span-id=%s", traceId, parentSpanId, spanId);
        String expectedB3TraceIdHeaderVal = traceId.replace("-", "");
        String expectedB3SpanIdHeaderVal = String.format("%016x", spanId);
        String expectedB3ParentSpanIdHeaderVal = String.format("%016x", parentSpanId);
        String expectedTraceParentHeaderVal = String.format("00-%s-%016x-00", traceId.replace("-", ""), spanId);

        underTest.intercept(httpRequest, new byte[0], clientHttpRequestExecution);

        verify(httpRequest).getHeaders();
        Assert.assertEquals(5, httpHeaders.size());
        Assert.assertEquals(expectedMoneyHeaderVal, httpHeaders.get("X-MoneyTrace").get(0));
        Assert.assertEquals(expectedB3TraceIdHeaderVal, httpHeaders.get("X-B3-TraceId").get(0));
        Assert.assertEquals(expectedB3ParentSpanIdHeaderVal, httpHeaders.get("X-B3-ParentSpanId").get(0));
        Assert.assertEquals(expectedB3SpanIdHeaderVal, httpHeaders.get("X-B3-SpanId").get(0));
        Assert.assertEquals(expectedTraceParentHeaderVal, httpHeaders.get("traceparent").get(0));
    }
}

