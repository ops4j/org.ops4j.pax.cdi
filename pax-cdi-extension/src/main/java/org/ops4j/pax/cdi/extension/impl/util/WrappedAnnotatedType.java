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

import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

public abstract class WrappedAnnotatedType<T> extends WrappedAnnotated implements AnnotatedType<T> {

    @Override
    public abstract AnnotatedType<T> delegate();

    @Override
    public Set<AnnotatedConstructor<T>> getConstructors() {
        return delegate().getConstructors();
    }

    @Override
    public Set<AnnotatedField<? super T>> getFields() {
        return delegate().getFields();
    }

    @Override
    public Class<T> getJavaClass() {
        return delegate().getJavaClass();
    }

    @Override
    public Set<AnnotatedMethod<? super T>> getMethods() {
        return delegate().getMethods();
    }
}
