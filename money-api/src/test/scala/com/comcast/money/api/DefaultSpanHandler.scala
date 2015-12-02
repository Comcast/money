package com.comcast.money.api

class DefaultSpanHandler extends SpanHandler {

  override def handle(spanData: SpanData): Unit = println(spanData.toString)
}
