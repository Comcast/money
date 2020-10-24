package com.comcast.money.core.samplers

import com.comcast.money.api.SpanId
import com.typesafe.config.Config


class TestSampler extends Sampler {
  override def shouldSample(spanId: SpanId, parentSpanId: Option[SpanId], spanName: String): SamplerResult = DropResult
}

class TestConfigurableSampler extends ConfigurableSampler {

  var calledConfigured = false

  override def configure(conf: Config): Unit = calledConfigured = true
  override def shouldSample(spanId: SpanId, parentSpanId: Option[SpanId], spanName: String): SamplerResult = DropResult
}
