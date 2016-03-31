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

package com.comcast.money.java;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.handlers.RequestHandler2;
import com.comcast.money.core.SpanId;
import com.comcast.money.internal.SpanLocal;
import com.comcast.money.japi.JMoney;

import scala.Option;

public class AmazonWebServiceClientMoneyTraceRequestHandler extends RequestHandler2 {

    public static final String MONEY_TRACE_HEADER = "X-MoneyTrace";

    private final String spanName;

    public AmazonWebServiceClientMoneyTraceRequestHandler(String spanName) {

        this.spanName = spanName;
    }

    @Override
    public void beforeRequest(Request<?> request) {

        if (!isMoneyEnabled()) {
            return;
        }

        startSpan(spanName);

        Option<SpanId> spanId = getCurrentSpan();
        if (spanId.isDefined()) {
            request.addHeader(MONEY_TRACE_HEADER, spanId.get().toHttpHeader());
        }
    }

    @Override
    public void afterResponse(Request<?> request, Response<?> response) {

        if (!isMoneyEnabled()) {
            return;
        }

        stopSpan(true);
    }

    @Override
    public void afterError(Request<?> request, Response<?> response, Exception e) {

        if (!isMoneyEnabled()) {
            return;
        }

        stopSpan(false);
    }

    protected boolean isMoneyEnabled() {

        return JMoney.isEnabled();
    }

    protected void startSpan(String spanName) {

        JMoney.startSpan(spanName);
    }

    protected void stopSpan(boolean status) {

        JMoney.stopSpan(status);
    }

    protected Option<SpanId> getCurrentSpan() {

        return SpanLocal.current();
    }
}
