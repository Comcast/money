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
 * Used to indicate that the code path should be traced and executes
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
 *     {@literal @}WithTracing
 *      public void measureMePlease() {
 *         // do something useful here
 *      }
 *     }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Timed {

    /**
     * Allows the developer to override the name that is used as the measurement key
     * for request tracing.  This must be provided.
     * <pre>
     *     {@code
     *     {@literal @}WithTracing("my.custom.trace.key")
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
