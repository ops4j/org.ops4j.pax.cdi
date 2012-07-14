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

/**
 * A dynamic proxy invocation handler which looks up a matching OSGi service for a CDI injection
 * point on each method invocation, possibly including a wait period as incicated by the
 * {@link OsgiService} qualifier.
 * 
 * @author Harald Wellmann
 * 
 */
public class DynamicInvocationHandler implements InvocationHandler {

    private InjectionPoint ip;

    public DynamicInvocationHandler(InjectionPoint ip) {
        this.ip = ip;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object service = InjectionPointOsgiUtils.lookupService(ip);
        return method.invoke(service, args);
    }
}
