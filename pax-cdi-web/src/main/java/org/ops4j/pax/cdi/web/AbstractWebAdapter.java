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

import javax.servlet.ServletContextListener;

import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.web.impl.CdiWebAppDependencyManager;
import org.ops4j.pax.swissbox.lifecycle.AbstractLifecycle;
import org.ops4j.pax.swissbox.tracker.ReplaceableService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public abstract class AbstractWebAdapter extends AbstractLifecycle {

    protected BundleContext bundleContext;
    private ServiceTracker<CdiContainer, CdiContainer> cdiContainerTracker;
    private CdiWebAppDependencyManager dependencyManager;
    private ReplaceableService<HttpService> httpService;

    protected AbstractWebAdapter(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    protected void onStart() {
        cdiContainerTracker = new ServiceTracker<CdiContainer, CdiContainer>(bundleContext,
            CdiContainer.class, new CdiContainerListener());
        cdiContainerTracker.open();

        dependencyManager = new CdiWebAppDependencyManager(bundleContext,
            getServletContextListener());

        httpService = new ReplaceableService<HttpService>(bundleContext, HttpService.class,
            dependencyManager);
        httpService.start();
    }

    protected abstract ServletContextListener getServletContextListener();

    @Override
    protected void onStop() {
        httpService.stop();
        cdiContainerTracker.close();
    }

    private class CdiContainerListener implements
        ServiceTrackerCustomizer<CdiContainer, CdiContainer> {

        @Override
        public CdiContainer addingService(ServiceReference<CdiContainer> reference) {
            CdiContainer cdiContainer = bundleContext.getService(reference);
            dependencyManager.addCdiContainer(cdiContainer);
            return cdiContainer;
        }

        @Override
        public void modifiedService(ServiceReference<CdiContainer> reference, CdiContainer service) {
            dependencyManager.removeCdiContainer(service);
            CdiContainer cdiContainer = bundleContext.getService(reference);
            dependencyManager.addCdiContainer(cdiContainer);
        }

        @Override
        public void removedService(ServiceReference<CdiContainer> reference, CdiContainer service) {
            dependencyManager.removeCdiContainer(service);
        }
    }
}
