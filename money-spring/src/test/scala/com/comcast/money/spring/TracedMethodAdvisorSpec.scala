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

package com.comcast.money.spring

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.springframework.aop.support.StaticMethodMatcherPointcut

class TracedMethodAdvisorSpec extends AnyWordSpec with Matchers with MockitoSugar {

  val springTracer = mock[SpringTracer]
  val interceptor = new TracedMethodInterceptor(springTracer)
  val advisor = new TracedMethodAdvisor(interceptor)

  "Trace Advisor" should {
    "bump up code coverage" in {
      advisor.getPointcut shouldBe a[StaticMethodMatcherPointcut]
      advisor.getAdvice shouldBe interceptor
    }
  }
}
