package com.comcast.money.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For use on method parameters where the method
 * is annotated with {@link Traced}
 * <p/>
 * By including this annotation on a parameter, the parameter name
 * and value will be added to the request trace before the method is called.
 * <p/>
 * This is equivalent to calling requestTracer.addTracingData at the beginning
 * of a method
 * <pre>
 *     {@code
 *     {@literal @}Traced
 *      public void measureMePlease(@TracedData("TraceMe") String traceMe) {
 *         // do something useful here
 *      }
 *     }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface TracedData {

    /**
     * Allows the developer to override the name that is used as the measurement key
     * for request tracing.
     * <pre>
     *     {@code
     *     {@literal @}Traced
     *      public void measureMePlease(@TracedData("CUSTOM_NAME") String traceMe) {
     *         // do something useful here
     *      }
     *     }
     * </pre>
     *
     * @return The value that is to be the measurement key for the trace
     */
    String value();

    /**
     * Allows the developer to indicate that this value should be propagated to children.
     * <pre>
     *     {@code
     *     {@literal @}Traced
     *      public void measureMePlease(@TracedData(value="CUSTOM_NAME", propagated=true) String traceMe) {
     *         // do something useful here
     *      }
     *     }
     * </pre>
     *
     * @return true if the note is propagated; false otherwise
     */
    boolean propagate() default false;
}