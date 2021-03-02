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
package org.ops4j.pax.cdi.spi;

import java.util.Map;
import java.util.WeakHashMap;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;

/**
 * Injector for unmanaged injection targets like servlets.
 *
 * @author Harald Wellmann
 */
public class Injector {

    private BeanManager beanManager;
    private Map<Class<?>, InjectionTarget<?>> injectionTargets = new WeakHashMap<>();
    private CdiContainer cdiContainer;

    /**
     * Creates an injector for the given CDI container.
     *
     * @param cdiContainer
     *            CDI container
     */
    public Injector(CdiContainer cdiContainer) {
        this.cdiContainer = cdiContainer;
        this.beanManager = cdiContainer.getBeanManager();
    }

    /**
     * Injects dependencies into an unmanaged target.
     *
     * @param target
     *            target instance
     */
    @SuppressWarnings({ "unchecked" })
    public <T> void inject(T target) {
        Class<T> klass = (Class<T>) target.getClass();
        InjectionTarget<T> it = getInjectionTarget(klass);
        CreationalContext<T> context = beanManager.createCreationalContext(null);
        it.inject(target, context);
        it.postConstruct(target);
    }

    private <T> InjectionTarget<T> getInjectionTarget(Class<T> klass) {

        @SuppressWarnings("unchecked")
        InjectionTarget<T> it = (InjectionTarget<T>) injectionTargets.get(klass);

        if (it == null) {
            it = createInjectionTarget(klass);
            injectionTargets.put(klass, it);
        }
        return it;
    }

    private <T> InjectionTarget<T> createInjectionTarget(Class<T> klass) {
        AnnotatedType<T> type = beanManager.createAnnotatedType(klass);
        InjectionTargetFactory<T> itFactory = beanManager.getInjectionTargetFactory(type);
        InjectionTarget<T> it = itFactory.createInjectionTarget(null);
        return it;
//        return getWrapper(klass).wrap(it);
    }

    @SuppressWarnings("unchecked")
    private <T> InjectionTargetWrapper<T> getWrapper(Class<T> klass) {
        return cdiContainer.getInstance().select(InjectionTargetWrapper.class).get();

    }

    /**
     * Destroy dependencies of an unmanaged target.
     *
     * @param instance
     *            target instance
     */
    @SuppressWarnings({ "unchecked" })
    public <T> void destroy(T instance) {
        if (instance != null) {
            Class<T> klass = (Class<T>) instance.getClass();
            InjectionTarget<T> it = getInjectionTarget(klass);
            it.preDestroy(instance);
            it.dispose(instance);
        }
    }
}
