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
import java.util.Iterator;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.util.TypeLiteral;

public class IterableInstance<T> implements Instance<T> {

    private final Iterable<T> iterable;

    public IterableInstance(Iterable<T> iterable) {
        this.iterable = iterable;
    }

    @Override
    public Instance<T> select(Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isUnsatisfied() {
        Iterator<T> iterator = iterator();
        return !iterator.hasNext();
    }

    @Override
    public boolean isAmbiguous() {
        Iterator<T> iterator = iterator();
        return iterator.hasNext() && iterator.next() != null && iterator.hasNext();
    }

    @Override
    public void destroy(T instance) {
    }

    @Override
    public Iterator<T> iterator() {
        return iterable.iterator();
    }

    @Override
    public T get() {
        Iterator<T> iterator = iterator();
        if (!iterator.hasNext()) {
            throw new UnsatisfiedResolutionException();
        }
        T value = iterator.next();
        if (iterator.hasNext()) {
            throw new AmbiguousResolutionException();
        }
        return value;
    }

}
