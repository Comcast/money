package com.comcast.money.core.samplers

import com.comcast.money.api.Sampler
import com.typesafe.config.Config

trait ConfigurableSampler extends Sampler {
  def configure(conf: Config): Unit
}
