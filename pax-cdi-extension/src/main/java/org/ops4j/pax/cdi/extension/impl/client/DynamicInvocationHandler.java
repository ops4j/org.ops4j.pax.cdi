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

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import javax.enterprise.inject.spi.InjectionPoint;

import org.ops4j.pax.cdi.extension.impl.compat.PrototypeScopeUtils;
import org.ops4j.pax.cdi.extension.impl.compat.ServiceObjectsWrapper;
import org.ops4j.pax.cdi.extension.impl.util.InjectionPointOsgiUtils;
import org.ops4j.pax.swissbox.core.ContextClassLoaderUtils;
import org.osgi.framework.ServiceReference;

/**
 * A dynamic proxy invocation handler which looks up a matching OSGi service for a CDI injection
 * point on each method invocation, possibly including a wait period as indicated by the
 * {@code OsgiService} qualifier.
 *
 * @param <S>
 *            service type
 *
 * @author Harald Wellmann
 *
 */
public class DynamicInvocationHandler<S> extends AbstractServiceInvocationHandler {

    /**
     * Creates an invocation handler for the given injection point.
     *
     * @param ip
     *            OSGi service injection point.
     */
    public DynamicInvocationHandler(InjectionPoint ip) {
        super(ip);
    }

    @Override
    // CHECKSTYLE:SKIP
    public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
        @SuppressWarnings("unchecked")
        ServiceReference<S> serviceRef = InjectionPointOsgiUtils.getServiceReference(ip);
        ServiceObjectsWrapper<S> serviceObjects = PrototypeScopeUtils.createServiceObjectsWrapper(
            bundleContext, serviceRef);
        final S service = serviceObjects.getService();
        Object result = ContextClassLoaderUtils.doWithClassLoader(
            cdiContainer.getContextClassLoader(), new Callable<Object>() {

                @Override
                public Object call() throws Exception {
                    return method.invoke(service, args);
                }
            });
        serviceObjects.ungetService(service);
        return result;
    }

    @Override
    public void release() {
        // not used
    }
}
