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
package org.ops4j.pax.cdi.jetty;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContextListener;

import org.ops4j.pax.cdi.jetty.impl.CdiServletContainerInitializer;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Harald Wellmann
 * 
 */
public abstract class CdiWebAppDependencyManager implements CdiContainerListener {

    private static Logger log = LoggerFactory.getLogger(CdiWebAppDependencyManager.class);

    private Map<Bundle, ServiceRegistration<ServletContainerInitializer>> registrations 
        = new HashMap<Bundle, ServiceRegistration<ServletContainerInitializer>>();

    protected abstract ServletContextListener getServletContextListener();

    private void register(Bundle bundle, CdiContainer cdiContainer) {
        CdiServletContainerInitializer initializer = new CdiServletContainerInitializer(
            cdiContainer, getServletContextListener());
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("org.ops4j.pax.cdi.bundle.id", bundle.getBundleId());
        ServiceRegistration<ServletContainerInitializer> registration = bundle.getBundleContext()
            .registerService(ServletContainerInitializer.class, initializer, props);
        registrations.put(bundle, registration);
        log.info("registered WebAppDependencyHolder for bundle [{}]", cdiContainer.getBundle());
    }

    private void unregister(Bundle bundle) {
        ServiceRegistration<ServletContainerInitializer> registration = registrations
            .remove(bundle);
        if (registration != null) {
            try {
                registration.unregister();
            }
            catch (IllegalStateException e) {
                // ignore if already unregistered
            }
        }
    }

    private boolean isWebBundle(Bundle bundle) {
        return bundle.getHeaders().get("Web-ContextPath") != null;
    }

    @Override
    public void postCreate(CdiContainer container) {
        Bundle bundle = container.getBundle();
        if (isWebBundle(bundle)) {
            register(bundle, container);
        }
    }

    @Override
    public void preDestroy(CdiContainer container) {
        unregister(container.getBundle());
    }
}
