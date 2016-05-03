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

package com.comcast.money.annotations;

import java.lang.annotation.*;

/**
 * Used to indicate that the code path should be traced.
 * <p>
 * This annotation can be used on any method that you want to trace.  Typically this will be
 * used on the controller to indicate a new request trace, as well as external service methods
 * <p>
 * Before the method is called, the a new trace will be started.  If there is an existing
 * trace in context, the existing trace will automatically become the parent trace for the new trace
 * After the method completes, even on exception, the tracer will stop tracing.
 * <p>
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

    /**
     * Allows the developer to specify an array of exception types to ignore.  If any of these exceptions
     * are encountered, then the span will close with a result of success=true.  Any exceptions not matching
     * the exceptions in this array will still close the exception with success=false
     * <pre>
     *     {@code
     *     {@literal @}Traced(value="my.custom.trace.key", ignoredExceptions={ IllegalArgumentException.class })
     *      public void measureMePlease() {
     *         // do something useful here
     *      }
     *     }
     * </pre>
     *
     * @return The array of exception classes that will be ignored
     */
    Class[] ignoredExceptions() default {};
}
