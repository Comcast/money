package com.comcast.money.spring3

import java.lang.reflect.Method

import com.comcast.money.annotations.Traced
import com.comcast.money.core.Result
import com.comcast.money.internal.MDCSupport
import com.comcast.money.logging.TraceLogging
import com.comcast.money.reflect.Reflections
import org.aopalliance.aop.Advice
import org.aopalliance.intercept.{MethodInterceptor, MethodInvocation}
import org.springframework.aop.Pointcut
import org.springframework.aop.support.{AbstractPointcutAdvisor, StaticMethodMatcherPointcut}
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.stereotype.Component

/**
 * Intercepts methods to start and stop a trace span around the method invocation
 */
@Component
class TracedMethodInterceptor @Autowired()(@Qualifier("springTracer") val tracer: SpringTracer)
  extends MethodInterceptor
  with Reflections with TraceLogging {

  val mdcSupport = new MDCSupport()

  override def invoke(invocation: MethodInvocation): AnyRef = {

    Option(invocation.getStaticPart.getAnnotation(classOf[Traced])) map { annotation =>
      var result = Result.success
      val oldSpanName = mdcSupport.getSpanNameMDC
      try {
        tracer.startSpan(annotation.value())
        mdcSupport.setSpanNameMDC(Some(annotation.value()))
        recordTracedParameters(invocation.getStaticPart.asInstanceOf[Method], invocation.getArguments, tracer)
        invocation.proceed()
      } catch {
        case t: Throwable =>
          logException(t)
          result = Result.failed
          throw t
      } finally {
        mdcSupport.setSpanNameMDC(oldSpanName)
        tracer.stopSpan(result)
      }
    } getOrElse {
      invocation.proceed()
    }
  }
}

/**
 * Used by spring so that only those methods that have the @Traced annotation
 * are actually advised
 */
@Component
class TracedMethodAdvisor @Autowired()(val interceptor: TracedMethodInterceptor) extends AbstractPointcutAdvisor {

  private val pointcut = new StaticMethodMatcherPointcut {
    override def matches(method: Method, targetClass: Class[_]): Boolean = method.isAnnotationPresent(classOf[Traced])
  }

  override def getPointcut: Pointcut = pointcut

  override def getAdvice: Advice = interceptor
}
