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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.ops4j.pax.cdi.api.Properties;
import org.ops4j.pax.cdi.api.Property;
import org.ops4j.pax.cdi.extension.impl.context.ServiceContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Harald Wellmann
 * 
 */
@ApplicationScoped
public class ComponentLifecycleManager implements ServiceTrackerCustomizer<Object, Void> {

    private static Logger log = LoggerFactory.getLogger(ComponentLifecycleManager.class);

    @Inject
    private BeanManager beanManager;

    /**
     * Registry for all service components of the current bean bundle.
     */
    @Inject
    private ComponentRegistry componentRegistry;

    /**
     * Bundle context of the current bean bundle.
     */
    @Inject
    private BundleContext bundleContext;

    /**
     * Service tracker tracking <em>all</em> services.
     */
    private ServiceTracker<Object, Void> tracker;

    /**
     * Service registrations for OSGi components. TODO Move this to ComponentDescriptor.
     */
    private List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();

    /**
     * Creational context for service components contextual instances.
     */
    private CreationalContext<Object> cc;

    /**
     * Service context for OSGi component {@code @ServiceScoped} contextual instances.
     */
    @Inject
    private ServiceContext context;

    /**
     * Starts the component lifecycle for this bean bundle.
     * <p>
     * Registers all satisfied components and starts a service tracker for unsatisfied dependencies.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void start() {
        cc = beanManager.createCreationalContext(null);

        // register services for all components that are satisfied already
        for (Bean bean : componentRegistry.getComponents()) {
            ComponentDescriptor descriptor = componentRegistry.getDescriptor(bean);
            if (descriptor.isSatisfied()) {
                log.info("component {} is available", bean);
                Object service = context.get(bean, cc);
                registerService(bean, service);
            }
        }

        // start a tracker with a catch-all filter to track services for unsatisfied dependencies
        try {
            Filter filter = bundleContext.createFilter("(objectClass=*)");
            tracker = new ServiceTracker<Object, Void>(bundleContext, filter, this);
            tracker.open();
        }
        catch (InvalidSyntaxException exc) {
            throw new Ops4jException(exc);
        }
    }

    /**
     * Stops the component lifecycle for the current bean bundle.
     * <p>
     * Closes the service tracker and unregisters all services for OSGi components.
     */
    public void stop() {
        tracker.close();
        for (ServiceRegistration<?> reg : registrations) {
            try {
                reg.unregister();
            }
            catch (IllegalStateException e) {
                // Ignore if the service has already been unregistered
            }
        }
        registrations.clear();
    }

    /**
     * Registers the OSGi service for the given OSGi component bean
     * 
     * @param bean
     *            OSGi component bean
     * @param service
     *            contextual instance of the given bean type
     */
    @SuppressWarnings("rawtypes")
    private void registerService(Bean bean, Object service) {
        // only register services for classes located in the extended bundle
        long extendedBundleId = bundleContext.getBundle().getBundleId();
        Class<?> klass = service.getClass();
        long serviceBundleId = FrameworkUtil.getBundle(klass).getBundleId();
        if (serviceBundleId != extendedBundleId) {
            return;
        }

        AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(klass);
        OsgiServiceProvider provider = annotatedType.getAnnotation(OsgiServiceProvider.class);

        String[] typeNames;
        if (provider.classes().length == 0) {
            typeNames = getTypeNamesForBeanTypes(bean);
        }
        else {
            typeNames = getTypeNamesForClasses(provider.classes());
        }

        Dictionary<String, Object> props = createProperties(klass, service);
        log.debug("publishing service {}, props = {}", typeNames[0], props);
        ServiceRegistration<?> reg = bundleContext.registerService(typeNames, service, props);
        registrations.add(reg);
    }

    private String[] getTypeNamesForBeanTypes(Bean<?> bean) {
        Set<Type> closure = bean.getTypes();
        String[] typeNames = new String[closure.size()];
        int i = 0;
        for (Type type : closure) {
            Class<?> c = (Class<?>) type;
            if (c.isInterface()) {
                typeNames[i++] = c.getName();
            }
        }
        if (i == 0) {
            typeNames[i++] = bean.getBeanClass().getName();
        }
        return Arrays.copyOf(typeNames, i);
    }

    private String[] getTypeNamesForClasses(Class<?>[] classes) {
        String[] typeNames = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            typeNames[i] = classes[i].getName();
        }
        return typeNames;
    }

    private Dictionary<String, Object> createProperties(Class<?> klass, Object service) {
        Properties props = klass.getAnnotation(Properties.class);
        if (props == null) {
            return null;
        }
        Hashtable<String, Object> dict = new Hashtable<String, Object>();
        for (Property property : props.value()) {
            dict.put(property.name(), property.value());
        }
        return dict;
    }

    /**
     * Checks if the given service reference matches an unsatisfied dependency. If all dependencies
     * are now satisfied, the component is instantiated and added to the context.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Void addingService(ServiceReference<Object> reference) {
        for (Bean bean : componentRegistry.getComponents()) {
            ComponentDescriptor descriptor = componentRegistry.getDescriptor(bean);
            if (!descriptor.isSatisfied()) {
                if (descriptor.checkDependencies(reference, bundleContext.getService(reference))) {
                    log.info("component {} is available", bean);
                    Object service = context.get(bean, cc);
                    registerService(bean, service);
                }
            }
        }
        return null;
    }

    @Override
    public void modifiedService(ServiceReference<Object> reference, Void unused) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removedService(ServiceReference<Object> reference, Void unused) {
        // TODO Auto-generated method stub

    }
}
