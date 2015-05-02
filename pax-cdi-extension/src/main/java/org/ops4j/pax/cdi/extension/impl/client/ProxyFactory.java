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
package org.ops4j.pax.cdi.extension.impl.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import javax.enterprise.inject.spi.InjectionPoint;

import org.ops4j.pax.cdi.api.OsgiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates OSGi service proxies for CDI injection points.
 *
 * @author Harald Wellmann
 *
 */
public class ProxyFactory {

    private static Logger log = LoggerFactory.getLogger(ProxyFactory.class);

    /**
     * Hidden constructor of utility class.
     */
    private ProxyFactory() {
    }

    /**
     * Creates a service proxy for the given {@link OsgiService} injection point.
     *
     * @param ip
     *            injection point
     * @return service proxy
     */
    public static <T> T getServiceProxy(InjectionPoint ip) {
        OsgiService qualifier = ip.getAnnotated().getAnnotation(OsgiService.class);
        log.debug("getting service proxy for {}, {} ", ip.getType(), qualifier);
        return createServiceProxy(ip);
    }

    @SuppressWarnings("unchecked")
    private static <T> T createServiceProxy(InjectionPoint ip) {
        Class<?> klass = (Class<?>) ip.getType();
        OsgiService os = ip.getAnnotated().getAnnotation(OsgiService.class);

        InvocationHandler handler = os.dynamic() ? new DynamicInvocationHandler<T>(ip)
            : new StaticInvocationHandler<T>(ip);

        ClassLoader classLoader = ip.getMember().getDeclaringClass().getClassLoader();
        return (T) Proxy.newProxyInstance(classLoader, new Class[] { klass }, handler);
    }
}
