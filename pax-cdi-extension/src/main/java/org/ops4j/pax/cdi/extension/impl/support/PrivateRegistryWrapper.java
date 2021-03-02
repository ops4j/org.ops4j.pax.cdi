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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class PrivateRegistryWrapper implements BundleContext {

    public static final String PRIVATE = "org.ops4j.pax.cdi.private";

    private final BundleContext delegate;
    private final List<PrivateServiceRegistration<?>> registrations = new CopyOnWriteArrayList<>();
    private final List<ListenerInfo> listeners = new CopyOnWriteArrayList<>();

    private final AtomicLong privateServiceId = new AtomicLong(0);

    public PrivateRegistryWrapper(BundleContext bundleContext) {
        this.delegate = bundleContext;
    }

    @Override
    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
        if (filter != null && filter.contains(PRIVATE)) {
            addPrivateServiceListener(listener, filter);
        } else {
            delegate.addServiceListener(listener, filter);
        }
    }

    @Override
    public void removeServiceListener(ServiceListener listener) {
        removePrivateServiceListener(listener);
        delegate.removeServiceListener(listener);
    }

    @Override
    public ServiceReference<?>[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        if (filter != null && filter.contains(PRIVATE)) {
            return getPrivateServiceReferences(clazz, filter);
        } else {
            return delegate.getServiceReferences(clazz, filter);
        }
    }

    @Override
    public ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties) {
        if (properties != null && properties.get(PRIVATE) != null) {
            return registerPrivate(clazzes, service, properties);
        } else {
            return delegate.registerService(clazzes, service, properties);
        }
    }

    @Override
    public <S> S getService(ServiceReference<S> reference) {
        if (reference instanceof PrivateServiceRegistration) {
            return getPrivateService(reference);
        } else {
            return delegate.getService(reference);
        }
    }

    @Override
    public boolean ungetService(ServiceReference<?> reference) {
        if (reference instanceof PrivateServiceRegistration) {
            return ungetPrivateService(reference);
        } else {
            return delegate.ungetService(reference);
        }
    }

    static class ListenerInfo {

        final ServiceListener listener;
        final Filter filter;

        ListenerInfo(ServiceListener listener, Filter filter) {
            this.listener = listener;
            this.filter = filter;
        }
    }

    private void addPrivateServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
        Filter flt = delegate.createFilter(filter);
        listeners.add(new ListenerInfo(listener, flt));
    }

    private void removePrivateServiceListener(ServiceListener listener) {
        listeners.removeIf(listenerInfo -> listenerInfo.listener == listener);
    }

    private <S> ServiceRegistration<S> registerPrivate(String[] clazzes, Object svc, Dictionary<String, ?> properties) {
        PrivateServiceRegistration<S> reg = new PrivateServiceRegistration<>(clazzes, svc, properties);
        registrations.add(reg);
        ServiceEvent event = new ServiceEvent(ServiceEvent.REGISTERED, reg);
        for (ListenerInfo li : listeners) {
            if (li.filter.match(reg)) {
                li.listener.serviceChanged(event);
            }
        }
        return reg;
    }

    private ServiceReference<?>[] getPrivateServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        Filter flt = delegate.createFilter(clazz != null ? "(&(objectClass=" + clazz + ")" + filter + ")" : filter);
        List<ServiceReference<?>> refs = new ArrayList<>();
        for (PrivateServiceRegistration<?> reg : registrations) {
            if (flt.match(reg)) {
                refs.add(reg);
            }
        }
        return refs.toArray(new ServiceReference[refs.size()]);
    }

    private <S> S getPrivateService(ServiceReference<S> reference) {
        PrivateServiceRegistration<S> reg = (PrivateServiceRegistration<S>) reference;
        if (reg.service instanceof ServiceFactory) {
            return (S) ((ServiceFactory) reg.service).getService(getBundle(), reg);
        }
        return (S) reg.service;
    }

    private <S> void unregisterPrivate(PrivateServiceRegistration<S> reg) {
        registrations.remove(reg);
        ServiceEvent event = new ServiceEvent(ServiceEvent.UNREGISTERING, reg);
        for (ListenerInfo li : listeners) {
            if (li.filter.match(reg)) {
                li.listener.serviceChanged(event);
            }
        }
    }

    private boolean ungetPrivateService(ServiceReference<?> reference) {
        return false;
    }

    @Override
    public void addServiceListener(ServiceListener listener) {
        delegate.addServiceListener(listener);
    }

    @Override
    public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties) {
        return delegate.registerService(clazz, service, properties);
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
        return delegate.registerService(clazz, service, properties);
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, ServiceFactory<S> factory, Dictionary<String, ?> properties) {
        return delegate.registerService(clazz, factory, properties);
    }

    @Override
    public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        return delegate.getAllServiceReferences(clazz, filter);
    }

    @Override
    public ServiceReference<?> getServiceReference(String clazz) {
        return delegate.getServiceReference(clazz);
    }

    @Override
    public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
        return delegate.getServiceReference(clazz);
    }

    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter) throws InvalidSyntaxException {
        return delegate.getServiceReferences(clazz, filter);
    }

    @Override
    public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) {
        return delegate.getServiceObjects(reference);
    }

    @Override
    public String getProperty(String key) {
        return delegate.getProperty(key);
    }

    @Override
    public Bundle getBundle() {
        return delegate.getBundle();
    }

    @Override
    public Bundle installBundle(String location, InputStream input) throws BundleException {
        return delegate.installBundle(location, input);
    }

    @Override
    public Bundle installBundle(String location) throws BundleException {
        return delegate.installBundle(location);
    }

    @Override
    public Bundle getBundle(long id) {
        return delegate.getBundle(id);
    }

    @Override
    public Bundle[] getBundles() {
        return delegate.getBundles();
    }

    @Override
    public void addBundleListener(BundleListener listener) {
        delegate.addBundleListener(listener);
    }

    @Override
    public void removeBundleListener(BundleListener listener) {
        delegate.removeBundleListener(listener);
    }

    @Override
    public void addFrameworkListener(FrameworkListener listener) {
        delegate.addFrameworkListener(listener);
    }

    @Override
    public void removeFrameworkListener(FrameworkListener listener) {
        delegate.removeFrameworkListener(listener);
    }

    @Override
    public File getDataFile(String filename) {
        return delegate.getDataFile(filename);
    }

    @Override
    public Filter createFilter(String filter) throws InvalidSyntaxException {
        return delegate.createFilter(filter);
    }

    @Override
    public Bundle getBundle(String location) {
        return delegate.getBundle(location);
    }

    class PrivateServiceRegistration<S> implements ServiceRegistration<S>, ServiceReference<S> {

        private final Hashtable<String, ?> properties;
        private final Object service;

        PrivateServiceRegistration(String[] clazzes, Object svc, Dictionary<String, ?> properties) {
            Hashtable<String, Object> props = new Hashtable<>();
            for (Enumeration<String> elem = properties.keys(); elem.hasMoreElements(); ) {
                String key = elem.nextElement();
                props.put(key, properties.get(key));
            }
            props.put(Constants.OBJECTCLASS, clazzes);
            props.put(Constants.SERVICE_ID, privateServiceId.decrementAndGet());
            this.properties = props;
            this.service = svc;
        }

        @Override
        public ServiceReference<S> getReference() {
            return this;
        }

        @Override
        public void setProperties(Dictionary<String, ?> properties) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unregister() {
            unregisterPrivate(this);
        }

        @Override
        public Object getProperty(String key) {
            return properties.get(key);
        }

        @Override
        public String[] getPropertyKeys() {
            Set<String> keys = properties.keySet();
            return keys.toArray(new String[keys.size()]);
        }

        @Override
        public Bundle getBundle() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle[] getUsingBundles() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAssignableTo(Bundle bundle, String className) {
            return true;
        }

        @Override
        public int compareTo(Object reference) {
            ServiceReference other = (ServiceReference) reference;

            Long id = (Long) getProperty(Constants.SERVICE_ID);
            Long otherId = (Long) other.getProperty(Constants.SERVICE_ID);

            if (id.equals(otherId)) {
                return 0; // same service
            }

            Object rankObj = getProperty(Constants.SERVICE_RANKING);
            Object otherRankObj = other.getProperty(Constants.SERVICE_RANKING);

            // If no rank, then spec says it defaults to zero.
            rankObj = (rankObj == null) ? new Integer(0) : rankObj;
            otherRankObj = (otherRankObj == null) ? new Integer(0) : otherRankObj;

            // If rank is not Integer, then spec says it defaults to zero.
            Integer rank = (rankObj instanceof Integer)
                    ? (Integer) rankObj : new Integer(0);
            Integer otherRank = (otherRankObj instanceof Integer)
                    ? (Integer) otherRankObj : new Integer(0);

            // Sort by rank in ascending order.
            if (rank.compareTo(otherRank) < 0) {
                return -1; // lower rank
            } else if (rank.compareTo(otherRank) > 0) {
                return 1; // higher rank
            }

            // If ranks are equal, then sort by service id in descending order.
            return (id.compareTo(otherId) < 0) ? 1 : -1;
        }

    }

}
