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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.ops4j.pax.cdi.api.BeanBundle;
import org.ops4j.pax.cdi.api.ContainerInitialized;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.osgi.framework.BundleContext;
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

    private BundleContext bundleContext;

    /**
     * Observes ContainerInitialized event and registers all OSGi service beans published by this
     * bundle.
     * 
     * @param event
     */
    public void onInitialized(@Observes ContainerInitialized event) {
        for (Object service : services) {
            Class<?> klass = service.getClass();
            int numSignatures = klass.getInterfaces().length + 1;
            String[] signatures = new String[numSignatures];
            signatures[0] = klass.getName();
            for (int i = 1; i < numSignatures; i++) {
                signatures[i] = klass.getInterfaces()[i - 1].getName();
            }
            log.debug("publishing service {}", signatures[0]);
            bundleContext.registerService(signatures, service, null);
        }
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
