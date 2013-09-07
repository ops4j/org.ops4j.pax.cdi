/*
 * Copyright 2013 Harald Wellmann
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Typed;

import org.ops4j.pax.cdi.api.ServiceScoped;

/**
 * Custom CDI context for OSGi service components.
 * 
 * @author Harald Wellmann
 * 
 */
@Typed()
public class ServiceContext implements Context {

    private Map<Contextual<?>, ServiceContextEntry<?>> serviceBeans = new ConcurrentHashMap<Contextual<?>, ServiceContextEntry<?>>();

    @Override
    public Class<? extends Annotation> getScope() {
        return ServiceScoped.class;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <T> T get(Contextual<T> component, CreationalContext<T> creationalContext) {
        ServiceContextEntry serviceBean = serviceBeans.get(component);
        if (serviceBean != null) {
            return (T) serviceBean.getContextualInstance();
        }

        T instance = component.create(creationalContext);
        serviceBean = new ServiceContextEntry(component, instance, creationalContext);
        serviceBeans.put(component, serviceBean);

        return instance;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <T> T get(Contextual<T> component) {
        ServiceContextEntry serviceBean = serviceBeans.get(component);
        if (serviceBean != null) {
            return (T) serviceBean.getContextualInstance();
        }
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void destroy(Contextual<?> component) {
        ServiceContextEntry serviceBean = serviceBeans.get(component);
        if (serviceBean != null) {
            Object instance = serviceBean.getContextualInstance();
            CreationalContext cc = serviceBean.getCreationalContext();
            serviceBean.getBean().destroy(instance, cc);
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
