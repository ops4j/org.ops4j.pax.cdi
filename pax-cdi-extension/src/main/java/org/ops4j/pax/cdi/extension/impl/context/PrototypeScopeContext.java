/*
 * Copyright 2014 Harald Wellmann
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

package org.ops4j.pax.cdi.extension.impl.context;

import java.lang.annotation.Annotation;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.BeanManager;

import org.ops4j.pax.cdi.api.PrototypeScoped;

/**
 * Custom CDI context for OSGi service components with prototype scope.
 *
 * @author Harald Wellmann
 *
 */
@Typed()
public class PrototypeScopeContext implements AlterableContext {

    private Map<Object, CreationalContext<?>> instanceMap = new IdentityHashMap<>();
    private BeanManager beanManager;

    private ThreadLocal<Object> service;

    public PrototypeScopeContext(BeanManager beanManager) {
        this.beanManager = beanManager;
        this.service = new ThreadLocal<>();
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return PrototypeScoped.class;
    }

    @Override
    public <T> T get(Contextual<T> component, CreationalContext<T> creationalContext) {
        T instance = component.create(creationalContext);
        instanceMap.put(instance, creationalContext);
        return instance;
    }

    @Override
    public <T> T get(Contextual<T> component) {
        return null;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public void destroy(Contextual<?> component) {
        Object instance = getService();
        CreationalContext<Object> cc = (CreationalContext<Object>) instanceMap.get(instance);
        if (cc != null) {
            ((Contextual<Object>) component).destroy(instance, cc);
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    public CreationalContext<?> getCreationalContext() {
        return beanManager.createCreationalContext(null);
    }

    @SuppressWarnings("unchecked")
    public <T> T getService() {
        return (T) service.get();
    }

    public <T> void setService(T service) {
        if (service == null) {
            this.service.remove();
        }
        else {
            this.service.set(service);
        }
    }
}
