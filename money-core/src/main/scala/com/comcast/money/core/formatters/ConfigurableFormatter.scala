package com.comcast.money.core.formatters

import com.typesafe.config.Config

trait ConfigurableFormatter extends Formatter {
  def configure(conf: Config): Unit
}
