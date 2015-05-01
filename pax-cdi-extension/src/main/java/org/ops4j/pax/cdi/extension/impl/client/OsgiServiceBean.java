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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.ops4j.pax.cdi.api.OsgiService;
import org.osgi.framework.ServiceException;

/**
 * Represents an OSGi service bean. Instances of a bean are proxied to an OSGi service. The service
 * is looked up per method invocation (dynamic = true) or once on bean instantiation (dynamic =
 * false)
 *
 * @author Harald Wellmann
 *
 * @param <T>
 */
public class OsgiServiceBean<T> implements Bean<T> {

    private Type type;
    private InjectionPoint ip;
    private OsgiService qualifier;

    public OsgiServiceBean(InjectionPoint injectionPoint) {
        this.ip = injectionPoint;
        this.type = ip.getType();
        this.qualifier = ip.getAnnotated().getAnnotation(OsgiService.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T create(CreationalContext<T> ctx) {
        try {
            return (T) ProxyFactory.getServiceProxy(ip);
        }
        catch (ServiceException exc) {
            throw new CreationException(exc);
        }
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        InvocationHandler handler = Proxy.getInvocationHandler(instance);
        if (handler instanceof AbstractServiceInvocationHandler) {
            AbstractServiceInvocationHandler serviceHandler = (AbstractServiceInvocationHandler) handler;
            serviceHandler.release();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<T> getBeanClass() {
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) type;
            return (Class<T>) ptype.getRawType();
        }
        return (Class<T>) type;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        Set<Annotation> s = new HashSet<>();
        s.add(new OsgiServiceQualifierType(qualifier));
        return s;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<Type> getTypes() {
        Set<Type> s = new HashSet<>();
        s.add(type);
        s.add(Object.class);
        return s;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public boolean isNullable() {
        return true;
    }
}
