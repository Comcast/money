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

package com.comcast.money.aspectj;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.comcast.money.annotations.TracedData;

public class ReflectionUtils {

    public List<TracedDataParameter> extractTracedParameters(Method method, Object[] args) {

        List<TracedDataParameter> tracedParameters = new ArrayList<TracedDataParameter>(args.length);
        if (args.length > 0) {
            Annotation[][] annotations = method.getParameterAnnotations();
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (annotations != null && annotations.length > 0) {
                for (int i = 0; i < annotations.length; i++) {
                    Annotation[] paramAnnotations = annotations[i];

                    // these are the annotations for a single parameter
                    if (paramAnnotations != null && paramAnnotations.length > 0) {
                        for (Annotation paramAnnotation : paramAnnotations) {
                            if (paramAnnotation instanceof TracedData) {
                                // we should only have one per, so only record the first
                                TracedData tracedData = (TracedData) paramAnnotation;
                                tracedParameters.add(new TracedDataParameter(tracedData, args[i], parameterTypes[i]));
                                break;
                            }
                        }
                    }
                }
            }
        }
        return tracedParameters;
    }
}
