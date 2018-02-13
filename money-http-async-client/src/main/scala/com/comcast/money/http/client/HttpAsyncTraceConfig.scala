package com.comcast.money.http.client

object HttpAsyncTraceConfig {
  lazy val HttpResponseTimeTraceKey: String = "http-call-duration"
  lazy val HttpFullResponseTimeTraceKey: String = "http-call-with-body-duration"
  lazy val ProcessResponseTimeTraceKey: String = "http-process-response-duration"
  lazy val HttpResponseCodeTraceKey: String = "http-response-code"
}