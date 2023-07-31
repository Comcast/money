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

package com.comcast.money.otel.handlers.otlp;

import java.time.Duration;
import java.util.Collection;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.comcast.money.api.SpanId;
import com.comcast.money.api.SpanInfo;
import com.comcast.money.otel.handlers.TestSpanInfo;
import com.comcast.money.otel.handlers.otlp.http.OtlpHttpHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        OtlpHttpSpanExporter.class,
        OtlpHttpSpanExporterBuilder.class
})
public class OtlpHttpHandlerSpec {

    private OtlpHttpSpanExporter spanExporter;
    private OtlpHttpSpanExporterBuilder spanExporterBuilder;

    private OtlpHttpHandler underTest;

    @Before
    public void beforeEach() throws Exception {
        PowerMockito.mockStatic(OtlpHttpSpanExporter.class);

        spanExporter = PowerMockito.mock(OtlpHttpSpanExporter.class);
        spanExporterBuilder = PowerMockito.mock(OtlpHttpSpanExporterBuilder.class);

        PowerMockito.when(OtlpHttpSpanExporter.builder()).thenReturn(spanExporterBuilder);
        PowerMockito.when(spanExporterBuilder.build()).thenReturn(spanExporter);
        PowerMockito.when(spanExporter.export(any())).thenReturn(CompletableResultCode.ofSuccess());
    }

    @Test
    public void configuresOtlpExporter() {
        Config config = ConfigFactory.parseString("batch = false");

        OtlpHttpHandler underTest = new OtlpHttpHandler(config);

        PowerMockito.verifyStatic(OtlpHttpSpanExporter.class);
        OtlpHttpSpanExporter.builder();
        Mockito.verify(spanExporterBuilder).build();

        SpanId spanId = SpanId.createNew();
        SpanInfo spanInfo = new TestSpanInfo(spanId);

        underTest.handle(spanInfo);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<SpanData>> captor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(spanExporter).export(captor.capture());
        Collection<SpanData> collection = captor.getValue();

        assertThat(collection).hasSize(1);
        SpanData spanData = collection.stream().findFirst().get();

        assertThat(spanData.getTraceId()).isEqualTo(spanId.traceIdAsHex());
        assertThat(spanData.getSpanId()).isEqualTo(spanId.selfIdAsHex());
    }

    @Test
    public void configuresOtlpExporterWithEndpoint() {
        Config config = ConfigFactory.parseString(
                "batch = false\n" +
                "exporter {\n" +
                "  endpoint = \"endpoint\"\n" +
                "}"
        );

        OtlpHttpHandler underTest = new OtlpHttpHandler(config);

        PowerMockito.verifyStatic(OtlpHttpSpanExporter.class);
        OtlpHttpSpanExporter.builder();
        Mockito.verify(spanExporterBuilder).setEndpoint("endpoint");
        Mockito.verify(spanExporterBuilder).build();
    }

    @Test
    public void configuresOtlpExporterWithCompression() {
        Config config = ConfigFactory.parseString(
                "batch = false\n" +
                        "exporter {\n" +
                        "  compression = \"gzip\"\n" +
                        "}"
        );

        OtlpHttpHandler underTest = new OtlpHttpHandler(config);

        PowerMockito.verifyStatic(OtlpHttpSpanExporter.class);
        OtlpHttpSpanExporter.builder();
        Mockito.verify(spanExporterBuilder).setCompression("gzip");
        Mockito.verify(spanExporterBuilder).build();
    }

    @Test
    public void configuresOtlpExporterWithDeadlineMillis() {
        Config config = ConfigFactory.parseString(
                "batch = false\n" +
                "exporter {\n" +
                "  timeout-ms = 500\n" +
                "}"
        );

        OtlpHttpHandler underTest = new OtlpHttpHandler(config);

        PowerMockito.verifyStatic(OtlpHttpSpanExporter.class);
        OtlpHttpSpanExporter.builder();
        Mockito.verify(spanExporterBuilder).setTimeout(Duration.ofMillis(500L));
        Mockito.verify(spanExporterBuilder).build();
    }
}
