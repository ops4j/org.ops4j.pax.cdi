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
package org.ops4j.pax.cdi.extension.impl.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

public class SimpleBean<T> implements Bean<T> {

    private final Class clazz;
    private final Class<? extends Annotation> scope;
    private final Supplier<T> supplier;
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;

    public SimpleBean(Class clazz, Class<? extends Annotation> scope, InjectionPoint ip, Supplier<T> supplier) {
        this(clazz, scope, Collections.singleton(ip.getType()), ip.getQualifiers(), supplier);
    }

    public SimpleBean(Class clazz, Class<? extends Annotation> scope, Set<Type> types, Set<Annotation> qualifiers, Supplier<T> supplier) {
        this.clazz = clazz;
        this.scope = scope;

        Set<Type> rawTypes = new HashSet<>();
        for (Type t : types) {
            if (t instanceof ParameterizedType) {
                Type raw = ((ParameterizedType)t).getRawType();
                if (raw == Instance.class) {
                    rawTypes.add(((ParameterizedType) t).getActualTypeArguments()[0]);
                } else {
                    rawTypes.add(raw);
                }
            } else {
                rawTypes.add(t);
            }
        }

        this.types = Collections.unmodifiableSet(rawTypes);
        this.qualifiers = Collections.unmodifiableSet(qualifiers);
        this.supplier = supplier;
    }

    @Override
    public Class<?> getBeanClass() {
        return clazz;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        return supplier.get();
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
    }
}
