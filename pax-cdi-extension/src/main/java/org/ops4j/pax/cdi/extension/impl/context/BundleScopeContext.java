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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.BeanManager;

import org.ops4j.pax.cdi.api.BundleScoped;
import org.osgi.framework.Bundle;

/**
 * Custom CDI context for OSGi service components.
 *
 * @author Harald Wellmann
 *
 */
@Typed()
public class BundleScopeContext implements Context {

    private Map<Contextual<?>, SingletonScopeContextEntry<?>> serviceBeans = new ConcurrentHashMap<Contextual<?>, SingletonScopeContextEntry<?>>();
    private BeanManager beanManager;

    private ThreadLocal<Bundle> clientBundle;
    private Map<Bundle, BeanMap> beanMaps;


    public BundleScopeContext(BeanManager beanManager) {
        this.beanManager = beanManager;
        this.clientBundle = new ThreadLocal<Bundle>();
        this.beanMaps = new HashMap<Bundle, BeanMap>();
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return BundleScoped.class;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <T> T get(Contextual<T> component, CreationalContext<T> creationalContext) {
        BeanMap beanMap = getBeanMap(creationalContext);


        SingletonScopeContextEntry serviceBean = beanMap.get(component);
        if (serviceBean != null) {
            return (T) serviceBean.getContextualInstance();
        }

        T instance = component.create(creationalContext);
        serviceBean = new SingletonScopeContextEntry(component, instance, beanMap.getCreationalContext());
        serviceBeans.put(component, serviceBean);

        return instance;
    }

    @SuppressWarnings({ "unchecked" })
    private <T> BeanMap getBeanMap(CreationalContext<T> creationalContext) {
        Bundle bundle = getClientBundle();
        if (bundle == null) {
            throw new ContextNotActiveException();
        }
        BeanMap beanMap = beanMaps.get(bundle);
        if (beanMap == null) {
            beanMap = new BeanMap();
            if (creationalContext == null) {
                beanMap.setCreationalContext(beanManager.createCreationalContext(null));
            }
            else {
                beanMap.setCreationalContext((CreationalContext<Object>) creationalContext);
            }
            beanMaps.put(bundle, beanMap);
        }
        return beanMap;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <T> T get(Contextual<T> component) {
        Bundle bundle = getClientBundle();
        if (bundle == null) {
            throw new ContextNotActiveException();
        }
        BeanMap beanMap = beanMaps.get(bundle);
        if (beanMap == null) {
            throw new ContextNotActiveException();
        }
        SingletonScopeContextEntry serviceBean = beanMap.get(component);
        if (serviceBean != null) {
            return (T) serviceBean.getContextualInstance();
        }
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void destroy(Contextual<?> component) {
        SingletonScopeContextEntry serviceBean = serviceBeans.get(component);
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

    /**
     * @return the clientBundle
     */
    public Bundle getClientBundle() {
        return clientBundle.get();
    }


    /**
     * @param clientBundle the clientBundle to set
     */
    public void setClientBundle(Bundle clientBundle) {
        if (clientBundle == null) {
            this.clientBundle.remove();
        }
        else {
            this.clientBundle.set(clientBundle);
        }
    }

    public CreationalContext<?> getCreationalContext() {
        return beanManager.createCreationalContext(null);
    }
}
