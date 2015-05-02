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
package org.ops4j.pax.cdi.extension.impl.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.inject.spi.BeanAttributes;

/**
 * Helper class for wrapping bean attributes.
 *
 * @param <T>
 *            bean type
 *
 * @author Harald Wellmann
 */
public abstract class WrappedBeanAttributes<T> implements BeanAttributes<T> {

    /**
     * Returns original bean attributes.
     *
     * @return attributes to be wrapped
     */
    protected abstract BeanAttributes<T> attributes();

    @Override
    public Set<Type> getTypes() {
        return attributes().getTypes();
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return attributes().getQualifiers();
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return attributes().getScope();
    }

    @Override
    public String getName() {
        return attributes().getName();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return attributes().getStereotypes();
    }

    @Override
    public boolean isAlternative() {
        return attributes().isAlternative();
    }

    @Override
    public int hashCode() {
        return attributes().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return attributes().equals(obj);
    }
}
