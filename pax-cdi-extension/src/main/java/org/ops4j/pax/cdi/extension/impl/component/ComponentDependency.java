/*
 * Copyright 2013 Harald Wellmann
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

package org.ops4j.pax.cdi.extension.impl.component;

import javax.enterprise.inject.spi.InjectionPoint;

import org.ops4j.pax.cdi.extension.impl.util.InjectionPointOsgiUtils;
import org.ops4j.pax.swissbox.lifecycle.AbstractLifecycle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * An OSGi service dependency of a given service component.
 *
 * @param <S>
 *            type of parent component
 * @param <T>
 *            type of child component
 *
 * @author Harald Wellmann
 */
public class ComponentDependency<S, T> extends AbstractLifecycle implements
    ServiceTrackerCustomizer<T, T> {

    /**
     * Injection point of this dependency. Must be qualified with {@code @OsgiService).

     */
    private InjectionPoint injectionPoint;

    /**
     * OSGi filter of the injection point.
     */
    private Filter filter;

    /**
     * Is this dependency satisfied (i.e. is there at least one matching service registered)?
     */
    private boolean satisfied;

    private ServiceTracker<T, T> tracker;

    private ComponentDescriptor<S> parent;

    private BundleContext bc;

    /**
     * Creates a component dependency for the given parent and injection point.
     *
     * @param parent
     *            descriptor of component containing the given injection point
     * @param ip
     *            injection point for service component dependency
     * @param filter
     *            OSGi service filter for injection point
     */
    public ComponentDependency(ComponentDescriptor<S> parent, InjectionPoint ip, Filter filter) {
        this.parent = parent;
        this.injectionPoint = ip;
        this.filter = filter;
    }

    /**
     * Gets the injection point.
     *
     * @return the injection point
     */
    public InjectionPoint getInjectionPoint() {
        return injectionPoint;
    }

    /**
     * Sets the injection point.
     *
     * @param injectionPoint
     *            the injection point to set
     */
    public void setInjectionPoint(InjectionPoint injectionPoint) {
        this.injectionPoint = injectionPoint;
    }

    /**
     * Gets the filter.
     *
     * @return the filter
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Sets the filter.
     *
     * @param filter
     *            the filter to set
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * Checks if this dependency is satisfied
     *
     * @return true if dependency is satisfied
     */
    public boolean isSatisfied() {
        return satisfied;
    }

    /**
     * Marks the dependency as satisfied or not.
     *
     * @param satisfied
     *            true if dependency is satisfied
     */
    public void setSatisfied(boolean satisfied) {
        this.satisfied = satisfied;
    }

    @Override
    protected void onStart() {
        bc = InjectionPointOsgiUtils.getBundleContext(injectionPoint);
        tracker = new ServiceTracker<T, T>(bc, filter, this);
        tracker.open();
    }

    @Override
    protected void onStop() {
        tracker.close();
    }

    @Override
    public T addingService(ServiceReference<T> reference) {
        if (!satisfied) {
            satisfied = true;
            parent.onDependencySatisfied();
            return bc.getService(reference);
        }
        return null;
    }

    @Override
    public void modifiedService(ServiceReference<T> reference, T service) {
        // not used
    }

    @Override
    public void removedService(ServiceReference<T> reference, T service) {
        if (satisfied) {
            satisfied = false;
            parent.onDependencyUnsatisfied();
        }
    }
}
