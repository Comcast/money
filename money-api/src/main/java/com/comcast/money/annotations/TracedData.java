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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For use on method parameters where the method
 * is annotated with {@link Traced}
 * <p>
 * By including this annotation on a parameter, the parameter name
 * and value will be added to the request trace before the method is called.
 * <p>
 * This is equivalent to calling requestTracer.addTracingData at the beginning
 * of a method
 * <pre>
 *     {@code
 *     {@literal @}Traced
 *      public void measureMePlease(@TracedData("TracMe") String traceMe) {
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
