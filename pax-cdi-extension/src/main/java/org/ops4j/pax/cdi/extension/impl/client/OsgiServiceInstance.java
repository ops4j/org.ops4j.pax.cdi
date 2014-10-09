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
package org.ops4j.pax.cdi.extension.impl.client;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Iterator;

import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.util.TypeLiteral;

import org.ops4j.pax.cdi.spi.util.Exceptions;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * A specialized {@link Instance} which looks up matching OSGi services from the service registry.
 *
 * @author Harald Wellmann
 *
 * @param <T>
 */
public class OsgiServiceInstance<T> implements Instance<T> {

    private BundleContext bc;
    private Class<T> klass;
    private String filter;

    public OsgiServiceInstance(BundleContext bc, Class<T> klass, String filter) {
        this.bc = bc;
        this.klass = klass;
        this.filter = filter.isEmpty() ? null : filter;
    }

    @Override
    public Iterator<T> iterator() {
        return new OsgiServiceIterator<T>(bc, getServiceReferences());
    }

    @Override
    public T get() {
        if (isUnsatisfied()) {
            throw new UnsatisfiedResolutionException();
        }
        if (isAmbiguous()) {
            throw new AmbiguousResolutionException();
        }
        return iterator().next();
    }

    @Override
    public Instance<T> select(Annotation... qualifiers) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isUnsatisfied() {
        Collection<ServiceReference<T>> refs = getServiceReferences();
        return refs.isEmpty();
    }

    private Collection<ServiceReference<T>> getServiceReferences() {
        try {
            Collection<ServiceReference<T>> refs = bc.getServiceReferences(klass, filter);
            return refs;
        }
        catch (InvalidSyntaxException exc) {
            throw Exceptions.unchecked(exc);
        }
    }

    @Override
    public boolean isAmbiguous() {
        return getServiceReferences().size() > 1;
    }
}
