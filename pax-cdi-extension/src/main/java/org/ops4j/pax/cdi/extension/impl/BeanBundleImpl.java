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

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.ops4j.pax.cdi.api.BeanBundle;
import org.ops4j.pax.cdi.api.ContainerInitialized;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.ops4j.pax.cdi.api.Properties;
import org.ops4j.pax.cdi.api.Property;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CDI bean representing a CDI enabled OSGi bundle, or bean bundle, for short.
 * <p>
 * Not intended to be used by application code. This bean is used internally to catch the
 * {@link ContainerInititialized} event and to publish CDI beans as OSGi services.
 * 
 * @author Harald Wellmann
 * 
 */
@ApplicationScoped
public class BeanBundleImpl implements BeanBundle {

    private static Logger log = LoggerFactory.getLogger(BeanBundleImpl.class);

    /**
     * All beans qualified as OSGi service provider. These beans will be registered as services.
     */
    @Inject
    @Any
    @OsgiServiceProvider
    private Instance<Object> services;
    
    @Inject
    private BeanManager beanManager;

    private BundleContext bundleContext;
    
    
    /**
     * Observes ContainerInitialized event and registers all OSGi service beans published by this
     * bundle.
     * 
     * @param event
     */
    public void onInitialized(@Observes ContainerInitialized event) {
        for (Object service : services) {
            registerService(service);
        }
    }

    private void registerService(Object service) {
        Class<?> klass = service.getClass();
        AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(klass);
        OsgiServiceProvider provider = annotatedType.getAnnotation(OsgiServiceProvider.class);
        
        String[] typeNames;        
        if (provider.classes().length == 0) {
            typeNames = getTypeNamesForTypeClosure(service, klass, annotatedType);
        }
        else {
            typeNames = getTypeNamesForClasses(provider.classes());
        }
        
        Dictionary<String, Object> props = createProperties(klass, service);
        log.debug("publishing service {}, props = {}", typeNames[0], props);
        getBundleContext(klass).registerService(typeNames, service, props);        
    }

    private String[] getTypeNamesForTypeClosure(Object service, Class<?> klass,
        AnnotatedType<?> annotatedType) {
        Set<Type> closure = annotatedType.getTypeClosure();
        String[] typeNames = new String[closure.size()];
        int i = 0;
        for (Type type : closure) {
            Class<?> c = (Class<?>) type;
            if (c.isInterface()) {
                typeNames[i++] = c.getName();
            }
        }
        if (i == 0) {
            typeNames[i++] = klass.getName();
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

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }
    
    private BundleContext getBundleContext(Class<?> klass) {
        BundleContext bc;
        try {
            BundleReference bundleRef = BundleReference.class.cast(klass.getClassLoader());
            bc = bundleRef.getBundle().getBundleContext();
            return bc;
        }
        catch (ClassCastException exc) {
            log.error("class " + klass.getName() + " is not loaded from an OSGi bundle");
            throw exc;
        }
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
