/*
 * Copyright 2014 Harald Wellmann.
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
package org.ops4j.pax.cdi.undertow.weld.impl;

import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Unmanaged;
import javax.enterprise.inject.spi.Unmanaged.UnmanagedInstance;

/**
 * Undertow instance factory for bean classes. This factory produces contextual instances
 * of beans.
 *
 * @author Harald Wellmann
 *
 * @param <T> bean type
 */
public class CdiInstanceFactory<T> implements InstanceFactory<T> {


    private BeanManager beanManager;
    private Class<T> klass;

    public CdiInstanceFactory(BeanManager beanManager, Class<T> klass) {
        this.beanManager = beanManager;
        this.klass = klass;
    }

    @Override
    public InstanceHandle<T> createInstance() throws InstantiationException {
        Unmanaged<T> unmanaged = new Unmanaged<T>(beanManager, klass);
        final UnmanagedInstance<T> unmanagedInstance = unmanaged.newInstance();
        final T instance = unmanagedInstance.produce().inject().postConstruct().get();

        return new InstanceHandle<T>() {

            @Override
            public T getInstance() {
                return instance;
            }

            @Override
            public void release() {
                unmanagedInstance.preDestroy().dispose();
            }
        };
    }
}
