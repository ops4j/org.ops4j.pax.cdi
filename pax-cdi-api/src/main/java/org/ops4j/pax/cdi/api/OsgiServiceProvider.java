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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

/**
 * Qualifier for automatic registration of CDI beans as OSGi service.
 * <p>
 * In application code, this qualifier shall only be used on classes and producer methods.
 * <p>
 * For each bean with this qualifier, a bean instance is automatically registered as an OSGi service
 * when the {@link ContainerInitialized} event has been observed.
 * <p>
 * The {@link #classes()} attribute denotes the classes or interfaces under which this service is
 * registered, defaulting to the interfaces the service class is assignable to, or to the service
 * class itself, if it is not assignable to any interface.
 * <p>
 * 
 * @author Harald Wellmann
 * 
 */
@Qualifier
@Target({ TYPE, METHOD, PARAMETER, FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OsgiServiceProvider {

    /**
     * The list of classes or interfaces under which the service is registered. The class annotated
     * by this qualifier must be assignable to each class in this list.
     * <p>
     * If this list is empty, the service will be registered for each interface it is assignable
     * to, or for the service class itself, if the service is not assignable to any interface.
     * @return
     */
    @Nonbinding
    Class<?>[] classes() default { };
}
