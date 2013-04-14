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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import org.ops4j.pax.cdi.api.OsgiService;
import org.ops4j.pax.cdi.extension.impl.util.InjectionPointOsgiUtils;
import org.osgi.framework.BundleContext;

/**
 * Wrapped {@link InjectionTarget} for OSGi services. Overrides injection into
 * Instance<T> when qualified as {@link OsgiService}.
 * 
 * @author Harald Wellmann
 *
 * @param <T>
 */
public class OsgiInjectionTarget<T> implements InjectionTarget<T> {

    private InjectionTarget<T> delegate;

    public OsgiInjectionTarget(InjectionTarget<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public T produce(CreationalContext<T> creationalContext) {
        return delegate.produce(creationalContext);
    }

    @Override
    public void dispose(T instance) {
        delegate.dispose(instance);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return delegate.getInjectionPoints();
    }

    @Override
    public void inject(T instance, CreationalContext<T> ctx) {
        delegate.inject(instance, ctx);
        for (InjectionPoint ip : delegate.getInjectionPoints()) {
            overrideInstanceInjection(instance, ip);
        }

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void overrideInstanceInjection(T beanInstance, InjectionPoint ip) {
        OsgiService qualifier = getOsgiServiceQualifier(ip);
        if (qualifier == null) {
            return;
        }
        Type instanceType = InjectionPointOsgiUtils.getInstanceType(ip);
        if (instanceType == null) {
            return;
        }

        BundleContext bc = InjectionPointOsgiUtils.getBundleContext(ip);
        OsgiServiceInstance instance = new OsgiServiceInstance(bc, (Class) instanceType,
            qualifier.filter());

        Annotated annotated = ip.getAnnotated();
        if (annotated instanceof AnnotatedField) {
            AnnotatedField annotatedField = (AnnotatedField) annotated;
            Field field = annotatedField.getJavaMember();
            field.setAccessible(true);
            try {
                field.set(beanInstance, instance);
            }
            catch (IllegalArgumentException exc) {
                throw new RuntimeException(exc);
            }
            catch (IllegalAccessException exc) {
                throw new RuntimeException(exc);
            }
        }

    }

    private OsgiService getOsgiServiceQualifier(InjectionPoint ip) {
        for (Annotation qualifier : ip.getQualifiers()) {
            if (qualifier instanceof OsgiService) {
                return (OsgiService) qualifier;
            }
        }
        return null;
    }

    @Override
    public void postConstruct(T instance) {
        delegate.postConstruct(instance);
    }

    @Override
    public void preDestroy(T instance) {
        delegate.preDestroy(instance);
    }
}
