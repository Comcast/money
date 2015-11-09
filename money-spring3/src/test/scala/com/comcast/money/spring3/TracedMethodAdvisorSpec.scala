package com.comcast.money.spring3

import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import org.springframework.aop.support.StaticMethodMatcherPointcut

class TracedMethodAdvisorSpec extends WordSpec with Matchers with MockitoSugar {

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
