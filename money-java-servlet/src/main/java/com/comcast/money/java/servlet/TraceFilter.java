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

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comcast.money.core.Money;
import com.comcast.money.core.SpanId;
import com.comcast.money.core.Tracer;

public class TraceFilter implements Filter {

    private static final String MONEY_TRACE_HEADER = "X-MoneyTrace";
    private static final Logger logger = LoggerFactory.getLogger(TraceFilter.class);

    protected Tracer tracer;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        tracer = Money.tracer;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequestWrapper httpRequest = new HttpServletRequestWrapper((HttpServletRequest)request);
        String moneyHttpHeader = httpRequest.getHeader(MONEY_TRACE_HEADER);

        if (moneyHttpHeader != null) {
            try {
                SpanId incomingSpanId = SpanId.fromHttpHeader(moneyHttpHeader);

                if (response instanceof HttpServletResponse) {
                    HttpServletResponse httpResponse = (HttpServletResponse)response;
                    httpResponse.addHeader(MONEY_TRACE_HEADER, moneyHttpHeader);
                }
                tracer.setTraceContext(incomingSpanId);
            } catch(Throwable ex) {
                logger.warn("Unable to parse money trace for request header '{}'", moneyHttpHeader);
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
