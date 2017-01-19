/*
 * Copyright 2012 Harald Wellmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.api;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation defining a single property for OSGi service registration. To be used as
 * a member of {@link Properties} in combination with {@link Service}.
 * 
 * @author Harald Wellmann
 */
@Target({ /* none */ })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Property {

    /** Property name. */
    String name();

    /** Property value. */
    String value();

    /** Property type. */
    String type() default "String";
}
