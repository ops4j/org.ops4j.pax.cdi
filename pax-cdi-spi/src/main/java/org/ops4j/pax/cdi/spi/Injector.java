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
package org.ops4j.pax.cdi.spi;

import java.util.Map;
import java.util.WeakHashMap;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

public class Injector {
    private BeanManager beanManager;
    private Map<Class<?>, InjectionTarget<?>> injectionTargets = new WeakHashMap<Class<?>, InjectionTarget<?>>();

    public Injector(BeanManager beanManager) {
        assert beanManager != null;
        this.beanManager = beanManager;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void inject(Object target) {
        Class<?> klass = target.getClass();
        InjectionTarget it = injectionTargets.get(klass);
        if (it == null) {
            it = beanManager.createInjectionTarget(beanManager.createAnnotatedType(klass));
            injectionTargets.put(klass, it);
        }
        CreationalContext<Object> context = beanManager.createCreationalContext(null);
        it.inject(target, context);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void destroy(Object instance) {
        if (instance != null) {
            Class<?> klass = instance.getClass();
            InjectionTarget it = beanManager.createInjectionTarget(beanManager.createAnnotatedType(klass));
            it.dispose(instance);
        }
    }
}
