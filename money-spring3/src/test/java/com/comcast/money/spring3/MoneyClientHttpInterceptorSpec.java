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

import com.comcast.money.api.Note;
import com.comcast.money.api.Span;
import com.comcast.money.api.SpanId;
import com.comcast.money.api.SpanInfo;
import com.comcast.money.core.internal.SpanLocal;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import java.util.Map;
import static org.mockito.Mockito.*;


public class MoneyClientHttpInterceptorSpec {

    private final static String expectedTraceId = "a";
    private final static Long expectedParentSpanId = 1L;
    private final static Long expectedSpanId = 2L;
    private final String expectedMoneyHeaderVal = String.format("trace-id=%s;parent-id=%s;span-id=%s", expectedTraceId, expectedParentSpanId, expectedSpanId);

    private HttpRequest httpRequest = mock(HttpRequest.class);
    private ClientHttpRequestExecution clientHttpRequestExecution = mock(ClientHttpRequestExecution.class);
    private Span span = mock(Span.class);

    private MoneyClientHttpRequestInterceptor moneyClientHttpRequestInterceptor = new MoneyClientHttpRequestInterceptor();


    @Before
    public void setUp() {
        when(span.info()).thenReturn(new TestSpanInfo());
        SpanLocal.push(span);
    }

    @After
    public void tearDown() {
        SpanLocal.clear();
    }

    @Test
    public void testInterceptor() throws Exception {

        HttpHeaders httpHeaders = new HttpHeaders();
        when(httpRequest.getHeaders()).thenReturn(httpHeaders);
        moneyClientHttpRequestInterceptor.intercept(httpRequest,"abc".getBytes(), clientHttpRequestExecution);
        verify(httpRequest).getHeaders();
        Assert.assertEquals(4,httpHeaders.size());
        Assert.assertEquals(expectedTraceId, httpHeaders.get("X-B3-TraceId").get(0));
        Assert.assertEquals(expectedParentSpanId.toString(), httpHeaders.get("X-B3-ParentSpanId").get(0));
        Assert.assertEquals(expectedSpanId.toString(), httpHeaders.get("X-B3-SpanId").get(0));
        Assert.assertEquals(expectedMoneyHeaderVal, httpHeaders.get("X-MoneyTrace").get(0));
    }

    private static class TestSpanInfo implements SpanInfo {
        @Override public Map<String, Note<?>> notes() {return null; }
        @Override public Long startTimeMillis() { return null;}
        @Override public Long startTimeMicros() { return null; }
        @Override public Long endTimeMillis() { return null; }
        @Override public Long endTimeMicros() { return null; }
        @Override public Boolean success() { return null; }
        @Override public SpanId id() { return new SpanId(expectedTraceId,expectedParentSpanId,expectedSpanId); }
        @Override public String name() { return null; }
        @Override public Long durationMicros() { return null; }
        @Override public String appName() { return null; }
        @Override public String host() { return null; }
    }
}
