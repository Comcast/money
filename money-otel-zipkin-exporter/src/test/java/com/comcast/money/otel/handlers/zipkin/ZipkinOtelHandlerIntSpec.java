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

package com.comcast.money.otel.handlers.zipkin;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import zipkin2.Span;
import zipkin2.junit.ZipkinRule;

import com.comcast.money.api.SpanId;
import com.comcast.money.api.SpanInfo;
import com.comcast.money.otel.handlers.TestSpanInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@RunWith(JUnit4.class)
public class ZipkinOtelHandlerIntSpec {

    @Rule
    public ZipkinRule zipkin = new ZipkinRule();

    @Test
    public void integrationWithZipkin() {
        String zipkinEndpoint = zipkin.httpUrl() + "/api/v2/spans";
        Config config = ConfigFactory.parseString(
                "batch = false\n" +
                        "export-only-sampled = true\n" +
                        "exporter {\n" +
                        "  service-name = \"myService\"\n" +
                        "  endpoint = \"" + zipkinEndpoint + "\"\n" +
                        "  encoder = \"json-v2\"\n" +
                        "}"
        );

        ZipkinOtelSpanHandler handler = new ZipkinOtelSpanHandler(config);

        SpanId spanId = SpanId.createNew();
        SpanInfo spanInfo = new TestSpanInfo(spanId);

        handler.handle(spanInfo);

        List<Span> trace = await().atMost(2, TimeUnit.SECONDS)
                .until(() -> zipkin.getTrace(spanId.traceIdAsHex()), Matchers.hasSize(1));

        assertThat(trace.get(0).id()).isEqualTo(spanInfo.id().selfIdAsHex());
    }
}
