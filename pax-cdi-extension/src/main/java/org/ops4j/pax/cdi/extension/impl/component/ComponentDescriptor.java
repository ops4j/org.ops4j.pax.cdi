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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.cdi.extension.impl.util.InjectionPointOsgiUtils;
import org.ops4j.pax.swissbox.lifecycle.AbstractLifecycle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.Service;

/**
 * Describes an OSGi service component and its dependencies.
 * 
 * @author Harald Wellmann
 * 
 */
public class ComponentDescriptor<S> extends AbstractLifecycle {

    /**
     * Dependencies of this component. There is one entry for each {@code @OsgiService} injection
     * point.
     */
    private List<ComponentDependency<S, ?>> dependencies = new ArrayList<ComponentDependency<S, ?>>();

    /**
     * Number of unsatisfied dependencies. The component is satisfied when there are no unsatisfied
     * dependencies.
     */
    private int numUnsatisfiedDependencies;
    
    private ServiceRegistration<S> serviceRegistration;

    private BundleContext bundleContext;
    
    private ComponentDependencyListener listener;

    private Bean<S> bean;
    
    
    /**
     * 
     */
    public ComponentDescriptor(Bean<S> bean, BundleContext bundleContext) {
        this.bean = bean;
        this.bundleContext = bundleContext;
    }

    /**
     * Checks if the component is satisfied.
     * 
     * @return
     */
    public boolean isSatisfied() {
        return numUnsatisfiedDependencies == 0;
    }

    /**
     * Adds a service dependency to the given component.
     * 
     * @param ip
     *            OSGi service injection point of the corresponding bean
     */
    public <T> void addDependency(InjectionPoint ip) {
        for (Annotation qualifier : ip.getQualifiers()) {
            if (qualifier instanceof Service) {
                Service svc = (Service) qualifier;
                if (!svc.required()) {
                    return;
                }
            }
        }
        String filterString = InjectionPointOsgiUtils.getFilter(ip);
        try {
            Filter filter = FrameworkUtil.createFilter(filterString);
            ComponentDependency<S, T> componentDependency = new ComponentDependency<S, T>(this, ip, filter);
            dependencies.add(componentDependency);
            numUnsatisfiedDependencies++;
        }
        catch (InvalidSyntaxException exc) {
            throw new Ops4jException(exc);
        }
    }
    
    public void onDependencySatisfied() {
        numUnsatisfiedDependencies--;
        if (isSatisfied()) {
            listener.onComponentSatisfied(this);
        }
    }
    
    public void onDependencyUnsatisfied() {
        boolean notifyListener = isSatisfied();
        numUnsatisfiedDependencies++;
        if (notifyListener && !isSatisfied()) {
            listener.onComponentUnsatisfied(this);            
        }        
    }

    
    /**
     * @return the serviceRegistration
     */
    public ServiceRegistration<S> getServiceRegistration() {
        return serviceRegistration;
    }

    
    /**
     * @param serviceRegistration the serviceRegistration to set
     */
    public void setServiceRegistration(ServiceRegistration<S> serviceRegistration) {
        this.serviceRegistration = serviceRegistration;
    }

    
    /**
     * @return the bundleContext
     */
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    
    
    /**
     * @param bundleContext the bundleContext to set
     */
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * @return the listener
     */
    public ComponentDependencyListener getListener() {
        return listener;
    }

    
    /**
     * @param listener the listener to set
     */
    public void setListener(ComponentDependencyListener listener) {
        this.listener = listener;
    }

    
    /**
     * @return the bean
     */
    public Bean<S> getBean() {
        return bean;
    }

    @Override
    protected void onStart() {
        for (ComponentDependency<S, ?> dependency : dependencies) {
            dependency.start();
        }
    }

    @Override
    protected void onStop() {
        listener.onComponentUnsatisfied(this);
        for (ComponentDependency<S, ?> dependency : dependencies) {
            dependency.stop();
        }        
    }
}
