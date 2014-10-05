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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedType;

public class AnnotatedTypeWrapper<T> extends WrappedAnnotatedType<T> {

    private AnnotatedType<T> delegate;
    private Set<Annotation> annotations;

    public AnnotatedTypeWrapper(AnnotatedType<T> delegate, Annotation... additionalAnnotations) {
        this.delegate = delegate;
        this.annotations = new HashSet<Annotation>(delegate.getAnnotations());
        for (Annotation annotation : additionalAnnotations) {
            annotations.add(annotation);
        }
        annotations = Collections.unmodifiableSet(annotations);
    }

    @Override
    public AnnotatedType<T> delegate() {
        return delegate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(annotationType)) {
                return (A) annotation;
            }
        }
        return null;
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return annotations;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return getAnnotation(annotationType) != null;
    }
}
