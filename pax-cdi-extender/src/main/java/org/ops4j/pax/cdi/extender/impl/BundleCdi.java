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
package org.ops4j.pax.cdi.extender.impl;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;

import org.ops4j.pax.cdi.spi.CdiContainer;

/**
 * Adapts {@link CdiContainer} to {@link CDI}.
 *
 * @param <T>
 *            type argument for bean instances

 * @author Harald Wellmann
 */
class BundleCdi<T> extends CDI<T> {

    private CdiContainer container;

    /**
     * Creates a {@link CDI} wrapper for the given container.
     *
     * @param container
     *            Pax CDI container
     */
    BundleCdi(CdiContainer container) {
        this.container = container;
    }

    @Override
    public Iterator<T> iterator() {
        return getInstance().iterator();
    }

    @Override
    public T get() {
        return getInstance().get();
    }

    @Override
    public Instance<T> select(Annotation... qualifiers) {
        return getInstance().select(qualifiers);
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        return getInstance().select(subtype, qualifiers);
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        return getInstance().select(subtype, qualifiers);
    }

    @Override
    public boolean isUnsatisfied() {
        return getInstance().isUnsatisfied();
    }

    @Override
    public boolean isAmbiguous() {
        return getInstance().isAmbiguous();
    }

    @Override
    public void destroy(T instance) {
        getInstance().destroy(instance);
    }

    private Instance<T> getInstance() {
        return container.getInstance();
    }

    @Override
    public BeanManager getBeanManager() {
        return container.getBeanManager();
    }

    @Override
    public String toString() {
        return String.format("[CDI container %s]", container.getBundle());

    }

    /**
     * Disposes the PAX CDI provider. Must be called when the Pax CDI extender is stopped.
     */
    static void dispose() {
        configuredProvider = null;
    }
}
