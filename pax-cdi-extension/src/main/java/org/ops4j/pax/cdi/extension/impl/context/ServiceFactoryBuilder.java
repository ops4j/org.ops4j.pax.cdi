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

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

/**
 * Builds service factories for OSGi service component beans, depending on the bean or service
 * scope.
 * <p>
 * This is the default builder for OSGi 4.3 and OSGi 5.0. On OSGi 6.0, a specialized builder is used
 * to handle the prototype scope.
 *
 * @author Harald Wellmann
 *
 */
public class ServiceFactoryBuilder {

    private BeanManager beanManager;

    /**
     * Constructs a service factory builder.
     *
     * @param beanManager
     *            bean manager of current bean bundle
     */
    public ServiceFactoryBuilder(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    /**
     * Builds a service factory for the given bean.
     *
     * @param bean
     *            OSGi service bean
     * @param <S>
     *            service type
     * @return service facto4ry
     */
    public <S> Object buildServiceFactory(Bean<S> bean) {
        Class<? extends Annotation> scope = bean.getScope();
        Context context = beanManager.getContext(scope);
        if (context instanceof PrototypeScopeContext) {
            PrototypeScopeContext prototypeScopeContext = (PrototypeScopeContext) context;
            return buildPrototypeScopeServiceFactory(prototypeScopeContext, bean);
        }
        if (context instanceof BundleScopeContext) {
            BundleScopeContext bundleScopeContext = (BundleScopeContext) context;
            return new BundleScopeServiceFactory<S>(bundleScopeContext, bean);
        }
        if (context instanceof SingletonScopeContext) {
            SingletonScopeContext singletonContext = (SingletonScopeContext) context;
            CreationalContext<S> cc = singletonContext.getCreationalContext();
            return singletonContext.get(bean, cc);
        }
        throw new IllegalStateException(bean.getBeanClass()
            + " does not have an OSGi compatible scope");
    }

    /**
     * Builds a service factory for a prototype scoped bean.
     *
     * @param context
     *            context of prototype scope
     * @param bean
     *            OSGi service bean
     * @return service factory.
     */
    protected <S> Object buildPrototypeScopeServiceFactory(PrototypeScopeContext context,
        Bean<S> bean) {
        throw new IllegalStateException("prototype scope not supported");
    }
}
