package com.comcast.money.api.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to indicate that the code path should be traced.
 * <p/>
 * This annotation can be used on any method that you want to trace.  Typically this will be
 * used on the controller to indicate a new request trace, as well as external service methods
 * <p/>
 * Before the method is called, the a new trace will be started.  If there is an existing
 * trace in context, the existing trace will automatically become the parent trace for the new trace
 * After the method completes, even on exception, the tracer will stop tracing.
 * <p/>
 * <pre>
 *     {@code
 *     {@literal @}Traced
 *      public void measureMePlease() {
 *         // do something useful here
 *      }
 *     }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Traced {

    /**
     * Allows the developer to override the name that is used as the measurement key
     * for request tracing.  This must be provided.
     * <pre>
     *     {@code
     *     {@literal @}Traced("my.custom.trace.key")
     *      public void measureMePlease() {
     *         // do something useful here
     *      }
     *     }
     * </pre>
     *
     * @return The value that is to be the measurement key for the trace
     */
    String value();
}