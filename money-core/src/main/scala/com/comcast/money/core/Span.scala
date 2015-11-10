package com.comcast.money.core

import scala.collection.Map

case class Span(
  spanId: SpanId,
  spanName: String,
  appName: String,
  host: String,
  startTime: Long,
  success: Boolean,
  duration: Long,
  notes: Map[String, Note[_]]
  )
