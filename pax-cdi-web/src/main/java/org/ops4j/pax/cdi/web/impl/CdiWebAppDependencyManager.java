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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletContextListener;

import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerListener;
import org.ops4j.pax.swissbox.tracker.ReplaceableServiceListener;
import org.ops4j.pax.web.service.WebAppDependencyHolder;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
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
public class CdiWebAppDependencyManager implements ReplaceableServiceListener<HttpService>,
    CdiContainerListener {

    private static Logger logger = LoggerFactory.getLogger(CdiWebAppDependencyManager.class);

    private BundleContext bundleContext;
    private Map<Long, ServiceRegistration<WebAppDependencyHolder>> registrations = new HashMap<Long, ServiceRegistration<WebAppDependencyHolder>>();
    private Map<Long, CdiContainer> webApps = new HashMap<Long, CdiContainer>();
    private HttpService httpService;
    private ServletContextListener servletContextListener;

    public CdiWebAppDependencyManager(BundleContext bundleContext, ServletContextListener listener) {
        this.bundleContext = bundleContext;
        this.servletContextListener = listener;
    }

    @Override
    public synchronized void serviceChanged(HttpService oldService, HttpService newService) {
        for (ServiceRegistration<WebAppDependencyHolder> registration : registrations.values()) {
            try {
                registration.unregister();
            }
            catch (IllegalStateException exc) {
                // ignore: service has been unregistered already
                // TODO Can we detect beforehand that the registration is no longer valid?
            }
        }
        httpService = newService;
        for (long bundleId : webApps.keySet()) {
            register(bundleId, webApps.get(bundleId));
        }
    }

    private void register(long bundleId, CdiContainer cdiContainer) {
        if (httpService != null) {

            HttpService webAppHttpService = getProxiedHttpService(bundleId);
            CdiServletContainerInitializer initializer = new CdiServletContainerInitializer(
                cdiContainer, servletContextListener);
            WebAppDependencyHolder dependencyHolder = new CdiWebAppDependencyHolder(
                webAppHttpService, initializer);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("bundle.id", Long.toString(bundleId));
            ServiceRegistration<WebAppDependencyHolder> registration = bundleContext
                .registerService(WebAppDependencyHolder.class, dependencyHolder, props);
            registrations.put(bundleId, registration);
            logger.info("registered WebAppDependencyHolder for bundle [{}]",
                cdiContainer.getBundle());
        }
    }

    /**
     * The HTTP Service is proxied per web app (TODO why?) - see {@link HttpServiceFactory} and its
     * use in the pax-web-runtime Activator. Since the proxied service also wraps the referencing
     * bundle, we make sure to obtain the correct proxy via the bundle context of the extended web
     * bundle instead of using our own {@code httpService} member which is registered to the
     * extender bundle.
     * 
     * @param bundleId
     *            bundle ID of extended web bundle
     * @return
     */
    private HttpService getProxiedHttpService(long bundleId) {
        Bundle webAppBundle = bundleContext.getBundle(bundleId);
        BundleContext webAppBundleContext = webAppBundle.getBundleContext();
        ServiceReference<HttpService> httpServiceRef = webAppBundleContext
            .getServiceReference(HttpService.class);
        HttpService webAppHttpService = webAppBundleContext.getService(httpServiceRef);
        return webAppHttpService;
    }

    private void unregister(long bundleId) {
        ServiceRegistration<WebAppDependencyHolder> registration = registrations.get(bundleId);
        if (registration != null) {
            try {
                registration.unregister();
            }
            catch (IllegalStateException exc) {
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
            long bundleId = bundle.getBundleId();
            webApps.put(bundleId, container);
            register(bundleId, container);
        }
    }

    @Override
    public synchronized void preDestroy(CdiContainer container) {
        long bundleId = container.getBundle().getBundleId();
        if (webApps.containsKey(bundleId)) {
            unregister(bundleId);
            webApps.remove(bundleId);
        }
    }
}
