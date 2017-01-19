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
package org.ops4j.pax.cdi.web;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiWebAdapter;
import org.ops4j.pax.cdi.web.impl.CdiServletContainerInitializer;
import org.ops4j.pax.cdi.web.impl.CdiWebAppDependencyHolder;
import org.ops4j.pax.web.service.WebAppDependencyHolder;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps track of runtime dependencies of web bean bundles and registers a
 * {@link WebAppDependencyHolder} for each web bean bundle as soon as all dependencies are
 * available.
 *
 * @author Harald Wellmann
 *
 */
public abstract class AbstractWebAppDependencyManager implements CdiWebAdapter, ServletContextListenerFactory {

    private static Logger log = LoggerFactory.getLogger(AbstractWebAppDependencyManager.class);

    private Map<Bundle, ServiceRegistration<WebAppDependencyHolder>> registrations = new HashMap<>();

    private void register(Bundle bundle, CdiContainer cdiContainer) {
        CdiServletContainerInitializer initializer = new CdiServletContainerInitializer(
            cdiContainer, this);
        WebAppDependencyHolder dependencyHolder = new CdiWebAppDependencyHolder(
            bundle.getBundleContext(), initializer);
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("bundle.id", Long.toString(bundle.getBundleId()));
        ServiceRegistration<WebAppDependencyHolder> registration = bundle.getBundleContext()
            .registerService(WebAppDependencyHolder.class, dependencyHolder, props);
        registrations.put(bundle, registration);
        log.info("registered WebAppDependencyHolder for bundle [{}]", cdiContainer.getBundle());
    }

    private void unregister(Bundle bundle) {
        ServiceRegistration<WebAppDependencyHolder> registration = registrations.remove(bundle);
        if (registration != null) {
            try {
                registration.unregister();
            }
            catch (IllegalStateException exc) {
                // ignore if already unregistered
                log.trace("service cannot be unregistered", exc);
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
        if (container != null) {
            unregister(container.getBundle());
        }
    }
}
