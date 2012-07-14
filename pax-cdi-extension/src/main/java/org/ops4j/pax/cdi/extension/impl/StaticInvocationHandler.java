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
package org.ops4j.pax.cdi.extension.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.enterprise.inject.spi.InjectionPoint;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;

/**
 * A static proxy invocation handler which always uses the same service reference obtained on
 * construction.
 * 
 * @author Harald Wellmann
 * 
 */
public class StaticInvocationHandler implements InvocationHandler {

    private InjectionPoint ip;

    private ServiceReference<?> serviceRef;
    private BundleContext bundleContext;

    public StaticInvocationHandler(InjectionPoint ip) {
        this.ip = ip;
        this.bundleContext = InjectionPointOsgiUtils.getBundleContext(ip);
        this.serviceRef = InjectionPointOsgiUtils.getServiceReference(ip);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (serviceRef != null) {
            Object service = bundleContext.getService(serviceRef);
            if (service != null) {
                return method.invoke(service, args);
            }
        }

        throw new ServiceException("no service for injection point " + ip,
            ServiceException.UNREGISTERED);
    }
}
