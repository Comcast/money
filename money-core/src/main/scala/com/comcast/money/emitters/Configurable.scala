package com.comcast.money.emitters

import com.typesafe.config.Config

trait Configurable {

  val conf: Config
}
