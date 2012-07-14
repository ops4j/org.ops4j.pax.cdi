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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Qualifier for OSGi service injection.
 * <p>
 * Injection points with this qualifier are injected with an OSGi bean which acts as a proxy to an
 * OSGi service obtained from the service registry. The service matches the type of the injection
 * point and any additional properties specified in the {@link #filter()} attribute.
 * <p>
 * Any matching service can be injected, the bundle that registered the service does not have to be
 * a bean bundle.
 * <p>
 * For the complementary action of publishing a CDI bean in the OSGi service registry, use the
 * {@link OsgiServiceProvider} qualifier.
 * 
 * @author Harald Wellmann
 * 
 */
@Qualifier
@Target({ TYPE, METHOD, PARAMETER, FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface OsgiService {

    /**
     * Indicates whether this proxy is static or dynamic.
     * <p>
     * A static proxy (the default) looks up a matching OSGi service once during the CDI bean
     * validation phase. If no service is found, bean validation fails. Otherwise, any bean method
     * invocation is forwarded to the services instance acquired during bean validation. If the
     * service has been unregistered, the beans throws an exception and will not try to acquire
     * another service instance.
     * 
     * @return
     */
    boolean dynamic() default false;

    /**
     * An LDAP filter in the usual OSGi syntax for narrowing down the set of matching OSGi services.
     * The {@code objectClass} property is always implicitly set to the type of the injection point.
     * 
     * @return
     */
    String filter() default "";

    /**
     * Timeout in milliseconds for obtaining a matching service. When {@link #dynamic()} is true,
     * this timeout applies to every method invocation of the injected bean: If a matching service
     * is aviable, the proxied method is invoked immediately. Otherwise, the proxy method blocks for
     * at most the given timeout period until a matching service has been acquired.
     * 
     * @return
     */
    int timeout() default 0;
}
