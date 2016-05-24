/*
 * Copyright 2016 Guillaume Nodet
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
package org.ops4j.pax.cdi.extension2;

import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ops4j.pax.cdi.api2.SingletonScoped;

public class SingletonContext implements AlterableContext {

    private Map<Contextual<?>, Holder<?>> store = new ConcurrentHashMap<>();

    @Override
    public Class<? extends Annotation> getScope() {
        return SingletonScoped.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        return (T) store.computeIfAbsent(contextual,
                c -> new Holder(c, creationalContext, c.create((CreationalContext) creationalContext))).instance;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> contextual) {
        Holder<?> h = store.get(contextual);
        return h != null ? (T) h.instance : null;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        Holder<?> h = store.remove(contextual);
        if (h != null) {
            h.destroy();
        }
    }

    public void destroy() {
        while (!store.isEmpty()) {
            destroy(store.keySet().iterator().next());
        }
    }

    static class Holder<T> {
        final Contextual<T> contextual;
        final CreationalContext<T> context;
        final T instance;

        public Holder(Contextual<T> contextual, CreationalContext<T> context, T instance) {
            this.contextual = contextual;
            this.context = context;
            this.instance = instance;
        }

        public void destroy() {
            this.contextual.destroy(instance, context);
        }
    }
}
