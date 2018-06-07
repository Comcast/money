/*
 * Copyright 2012-2015 Comcast Cable Communications Management, LLC
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

package com.comcast.money.spring3

import com.comcast.money.core.Formatters
import com.comcast.money.core.internal.SpanLocal
import org.springframework.http.HttpRequest
import org.springframework.http.client.{ ClientHttpRequestExecution, ClientHttpRequestInterceptor, ClientHttpResponse }
import org.springframework.stereotype.Component

/**
 * An Http Request interceptor implementation that adds X-B3 style distributed trace headers
 * to http requests made to other services.
 * This is defined as a spring component and designed to wired in to spring applications.
 * <p>
 *
 *   For example, the following code will add X-B3 headers to restful http requests using "MyTemplate"
 * <pre>
 *
 * {@literal @}SpringBootApplication
 * {@literal @}ComponentScan (basePackages = {"com.comcast.money.spring3"})
 *  public class Application {
 *
 *    {@literal @}Autowired MoneyClientHttpRequestInterceptor moneyClientHttpRequestInterceptor;
 *
 *    {@literal @}Bean ("MyTemplate")
 *     public RestTemplate restTemplate(RestTemplateBuilder builder) {
 *        return builder.additionalInterceptors(moneyClientHttpRequestInterceptor).build();
 *     }
 * }
 *
 *
 * {@literal @}RestController
 *  public class HelloController {
 *
 *   {@literal @}Autowired
 *   {@literal @}Qualifier("MyTemplate")
 *    private RestTemplate myTemplate;
 *
 *
 *   {@literal @}RequestMapping("/annotated")
 *   {@literal @}Traced("echo from  httpbin.org/headers")
 *    public String annotated() {
 *         myTemplate.getForObject("http://httpbin.org/headers", String.class);
 *         return "Used the {@literal @}Traced annotation!";
 *   }
 *
 * }
 * </pre>
 *
 */
@Component
class MoneyClientHttpRequestInterceptor extends ClientHttpRequestInterceptor {
  override def intercept(httpRequest: HttpRequest, body: Array[Byte], clientHttpRequestExecution: ClientHttpRequestExecution): ClientHttpResponse = {
    SpanLocal.current foreach { span =>
      val headers = httpRequest.getHeaders
      headers.add("X-MoneyTrace", Formatters.toHttpHeader(span.info.id))
      Formatters.toB3Headers(span.info.id)(
        headers.add("X-B3-TraceId", _),
        headers.add("X-B3-ParentSpanId", _),
        headers.add("X-B3-SpanId", _)
      )
    }
    clientHttpRequestExecution.execute(httpRequest, body)
  }
}
