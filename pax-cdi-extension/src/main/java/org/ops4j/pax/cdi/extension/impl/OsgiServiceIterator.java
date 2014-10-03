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
package org.ops4j.pax.cdi.extension.impl;

import java.util.Collection;
import java.util.Iterator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


/**
 * Iterates over all services for a given collection of service references.
 *
 * @author Harald Wellmann
 *
 * @param <T>
 */
public class OsgiServiceIterator<T> implements Iterator<T> {

    private BundleContext bc;
    private Iterator<ServiceReference<T>> refIt;

    public OsgiServiceIterator(BundleContext bc, Collection<ServiceReference<T>> references) {
        this.bc = bc;
        this.refIt = references.iterator();
    }

    @Override
    public boolean hasNext() {
        return refIt.hasNext();
    }

    @Override
    public T next() {
        ServiceReference<T> ref = refIt.next();
        return bc.getServiceObjects(ref).getService();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
