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

package com.comcast.money.java.servlet;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.comcast.money.core.SpanId;
import com.comcast.money.core.Tracer;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TraceFilterTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private FilterChain mockFilterChain;

    @Mock
    private Tracer mockTracer;

    private SpanId incomingSpanId = new SpanId();

    private TraceFilter underTest = new TraceFilter() {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            tracer = mockTracer;
        }
    };

    @Before
    public void setUp() throws Exception {
        underTest.init(null);
    }

    @Test
    public void testMoneyHttpHeaderPresent() throws Exception {

        when(mockRequest.getHeader("X-MoneyTrace")).thenReturn(toHttpHeader(incomingSpanId));

        underTest.doFilter(mockRequest, mockResponse, mockFilterChain);

        verify(mockResponse).addHeader("X-MoneyTrace", toHttpHeader(incomingSpanId));
        verify(mockTracer).setTraceContext(incomingSpanId);
    }

    @Test
    public void testMoneyHttpHeaderNotPresent() throws Exception {
        when(mockRequest.getHeader("X-MoneyTrace")).thenReturn(null);

        underTest.doFilter(mockRequest, mockResponse, mockFilterChain);

        verifyZeroInteractions(mockTracer);
        verify(mockResponse, never()).addHeader("X-MoneyTrace", toHttpHeader(incomingSpanId));
    }

    @Test
    public void testResponseIsNotHttpServletResponse() throws Exception {

        ServletResponse notHttpServletResponse = mock(ServletResponse.class);
        when(mockRequest.getHeader("X-MoneyTrace")).thenReturn(toHttpHeader(incomingSpanId));

        underTest.doFilter(mockRequest, notHttpServletResponse, mockFilterChain);

        verify(mockTracer).setTraceContext(incomingSpanId);
        verifyZeroInteractions(notHttpServletResponse);
    }

    @Test
    public void testFilterChainIsCalledOnMoneyHeaderParseException() throws Exception {

        when(mockRequest.getHeader("X-MoneyTrace")).thenReturn("this ain't right");

        underTest.doFilter(mockRequest, mockResponse, mockFilterChain);

        verifyZeroInteractions(mockTracer);
        verifyZeroInteractions(mockResponse);
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    }

    private String toHttpHeader(SpanId spanId) {
        final String MONEY_HEADER_FORMAT = "trace-id=%s;parent-id=%s;span-id=%s";
        return String.format(MONEY_HEADER_FORMAT, spanId.getTraceId(), spanId.getParentId(), spanId.getSelfId());
    }
}
