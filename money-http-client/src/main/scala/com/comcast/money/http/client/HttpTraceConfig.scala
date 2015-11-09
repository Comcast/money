package com.comcast.money.http.client

import com.comcast.money.core.Money

object HttpTraceConfig {

  lazy val HttpResponseTimeTraceKey: String = Money.config.getString("money.http-client.metric-names.http-call-duration")
  lazy val HttpFullResponseTimeTraceKey: String = Money.config.getString("money.http-client.metric-names.http-call-with-body-duration")
  lazy val ProcessResponseTimeTraceKey: String = Money.config.getString("money.http-client.metric-names.http-process-response-duration")
  lazy val HttpResponseCodeTraceKey: String = Money.config.getString("money.http-client.metric-names.http-response-code")
}
