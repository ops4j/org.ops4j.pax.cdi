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
    
    
    private Iterator<ServiceReference<T>> refIt;

    public OsgiServiceIterator(Collection<ServiceReference<T>> references) {
        this.refIt = references.iterator();
    }

    @Override
    public boolean hasNext() {
        return refIt.hasNext();
    }

    @Override
    public T next() {
        ServiceReference<T> ref = refIt.next();
        BundleContext bc = ref.getBundle().getBundleContext();
        return bc.getService(ref);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
