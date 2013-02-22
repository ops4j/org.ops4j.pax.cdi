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

import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.cdi.web.impl.CdiWebAppDependencyManager;
import org.ops4j.pax.swissbox.lifecycle.AbstractLifecycle;
import org.ops4j.pax.swissbox.tracker.ReplaceableService;
import org.ops4j.pax.swissbox.tracker.ReplaceableServiceListener;
import org.osgi.framework.BundleContext;

/**
 * Abstract base class for Pax CDI web adapters. For each CDI provider with web support, a derived
 * class needs to implement {@link #getServletContextListener()} and call {@link #start()} or
 * {@link #stop()} when the provider dependent adapter bundle is started or stopped.
 * 
 * @author Harald Wellmann
 * 
 */
public abstract class AbstractWebAdapter extends AbstractLifecycle implements
    ReplaceableServiceListener<CdiContainerFactory> {

    protected BundleContext bundleContext;
    private CdiWebAppDependencyManager dependencyManager;
    private ReplaceableService<CdiContainerFactory> cdiContainerFactory;

    protected AbstractWebAdapter(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    protected void onStart() {
        dependencyManager = new CdiWebAppDependencyManager(getServletContextListener());
        cdiContainerFactory = new ReplaceableService<CdiContainerFactory>(bundleContext, CdiContainerFactory.class, this);
        cdiContainerFactory.start();
    }

    @Override
    public synchronized void serviceChanged(CdiContainerFactory oldService, CdiContainerFactory newService) {
        if (oldService != null) {
            oldService.removeListener(dependencyManager);
            dependencyManager.unregisterAll();
        }
        if (newService != null) {
            newService.addListener(dependencyManager);
        }
    }

    protected abstract ServletContextListener getServletContextListener();

    @Override
    protected void onStop() {
        cdiContainerFactory.stop();
    }
}
