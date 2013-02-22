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
package org.ops4j.pax.cdi.web.impl;

import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerListener;
import org.ops4j.pax.web.service.WebAppDependencyHolder;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextListener;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Keeps track of runtime dependencies of web bean bundles and registers a
 * {@link WebAppDependencyHolder} for each web bean bundle as soon as all dependencies are
 * available.
 * 
 * @author Harald Wellmann
 * 
 */
public class CdiWebAppDependencyManager implements CdiContainerListener {

    private static Logger logger = LoggerFactory.getLogger(CdiWebAppDependencyManager.class);

    private Map<Bundle, ServiceRegistration<WebAppDependencyHolder>> registrations = new HashMap<Bundle, ServiceRegistration<WebAppDependencyHolder>>();
    private ServletContextListener servletContextListener;

    public CdiWebAppDependencyManager(ServletContextListener listener) {
        this.servletContextListener = listener;
    }

    private void register(Bundle bundle, CdiContainer cdiContainer) {
        CdiServletContainerInitializer initializer = new CdiServletContainerInitializer(
            cdiContainer, servletContextListener);
        WebAppDependencyHolder dependencyHolder = new CdiWebAppDependencyHolder(
            bundle.getBundleContext(), initializer);
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("bundle.id", Long.toString(bundle.getBundleId()));
        ServiceRegistration<WebAppDependencyHolder> registration = bundle.getBundleContext()
            .registerService(WebAppDependencyHolder.class, dependencyHolder, props);
        registrations.put(bundle, registration);
        logger.info("registered WebAppDependencyHolder for bundle [{}]", cdiContainer.getBundle());
    }

    private void unregister(Bundle bundle) {
        ServiceRegistration<WebAppDependencyHolder> registration = registrations.remove(bundle);
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
        return bundle.getHeaders().get(WebContainerConstants.CONTEXT_PATH_KEY) != null;
    }

    @Override
    public synchronized void postCreate(CdiContainer container) {
        Bundle bundle = container.getBundle();
        if (isWebBundle(bundle)) {
            register(bundle, container);
        }
    }

    @Override
    public synchronized void preDestroy(CdiContainer container) {
        unregister(container.getBundle());
    }

    public synchronized void unregisterAll() {
        while (!registrations.isEmpty()) {
            unregister(registrations.keySet().iterator().next());
        }
    }
}
