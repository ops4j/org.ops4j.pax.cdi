/*
 * Copyright 2021 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.extension.impl.context;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

/**
 * An entry in the {@link SingletonScopeContext}, wrapping a managed bean, its contextual instance
 * and the creational context for this instance.
 *
 * @author Harald Wellmann
 *
 * @param <T>
 */
public class SingletonScopeContextEntry<T> {

    private Contextual<T> bean;
    private T contextualInstance;
    private CreationalContext<T> creationalContext;

    /**
     * Creates a context entry for the given bean, an intance of this bean and its creational
     * context.
     *
     * @param bean
     *            bean with OSGi singleton scope
     * @param contextualInstance
     *            bean instance
     * @param creationalContext
     *            creational context of instance
     */
    public SingletonScopeContextEntry(Contextual<T> bean, T contextualInstance,
        CreationalContext<T> creationalContext) {

        this.bean = bean;
        this.contextualInstance = contextualInstance;
        this.creationalContext = creationalContext;
    }

    public Contextual<T> getBean() {
        return bean;
    }

    public T getContextualInstance() {
        return contextualInstance;
    }

    public CreationalContext<T> getCreationalContext() {
        return creationalContext;
    }
}
