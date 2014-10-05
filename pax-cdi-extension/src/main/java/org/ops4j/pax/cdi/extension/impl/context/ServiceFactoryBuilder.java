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

import org.ops4j.pax.cdi.extension.impl.component.ComponentDescriptor;


public class ServiceFactoryBuilder {


    private BeanManager beanManager;

    public ServiceFactoryBuilder(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    public <S> Object buildServiceFactory(ComponentDescriptor<S> descriptor) {
        Bean<S> bean = descriptor.getBean();
        Class<? extends Annotation> scope = bean.getScope();
        Context context = beanManager.getContext(scope);
        if (context instanceof PrototypeScopeContext) {
            PrototypeScopeContext prototypeScopeContext = (PrototypeScopeContext) context;
            return new PrototypeScopeServiceFactory<S>(prototypeScopeContext, bean);
        }
        if (context instanceof BundleScopeContext) {
            BundleScopeContext bundleScopeContext = (BundleScopeContext) context;
            return new BundleScopeServiceFactory<S>(bundleScopeContext, bean);
        }
        if (context instanceof ServiceContext) {
            ServiceContext singletonContext = (ServiceContext) context;
            CreationalContext<S> cc = singletonContext.getCreationalContext();
            return singletonContext.get(bean, cc);
        }
        throw new IllegalStateException(bean.getBeanClass() + " does not have an OSGi compatible scope");
    }
}
