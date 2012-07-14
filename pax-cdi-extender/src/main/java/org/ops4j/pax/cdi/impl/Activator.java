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
package org.ops4j.pax.cdi.impl;

import java.net.URL;

import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.swissbox.extender.BundleURLScanner;
import org.ops4j.pax.swissbox.extender.BundleWatcher;
import org.ops4j.pax.swissbox.tracker.ReplaceableService;
import org.ops4j.pax.swissbox.tracker.ReplaceableServiceListener;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activates the CDI extender bundle which observes {@link CdiContainerFactory} services, bean
 * bundles and CDI extension bundles.
 * <p>
 * This bundle uses the OSGi extender pattern to enrich every bean bundle with a CDI container. A
 * bean bundle is an OSGi bundle which also qualifies as a CDI bean deployment archive as indicated
 * by a {@code beans.xml} descriptor.
 * <p>
 * The extension process requires a {@code CdiContainerFactory} to be available. At the moment, use
 * start levels to ensure this.
 * <p>
 * TODO More flexible handling of service dynamics.
 * 
 * @author Harald Wellmann
 * 
 */
public class Activator implements BundleActivator, ReplaceableServiceListener<CdiContainerFactory> {

    private static Logger log = LoggerFactory.getLogger(Activator.class);

    /** Context of this bundle. */
    private BundleContext bc;

    /** Bundle watcher for bean bundles. */
    private BundleWatcher<URL> beanBundleWatcher;

    /** Bundle watcher for CDI extension bundles. */
    private BundleWatcher<URL> extensionWatcher;

    /** Service handle for a CDI container factory. */
    private ReplaceableService<CdiContainerFactory> replaceableService;
    private CdiExtensionObserver extensionObserver;

    @SuppressWarnings("unchecked")
    public void start(BundleContext bc) throws Exception {
        log.debug("starting bundle {}", bc.getBundle().getSymbolicName());

        this.bc = bc;
        replaceableService = new ReplaceableService<CdiContainerFactory>(bc,
            CdiContainerFactory.class, this);
        replaceableService.start();

        BundleURLScanner scanner = new BundleURLScanner("META-INF/services",
            "javax.enterprise.inject.spi.Extension", false);
        extensionObserver = new CdiExtensionObserver();
        extensionWatcher = new BundleWatcher<URL>(bc, scanner, extensionObserver);
        extensionWatcher.start();

    }

    public void stop(BundleContext context) throws Exception {
        log.debug("stopping bundle {}", context.getBundle().getSymbolicName());
        if (beanBundleWatcher != null) {
            beanBundleWatcher.stop();
        }
        replaceableService.stop();
        extensionWatcher.stop();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void serviceChanged(CdiContainerFactory oldService, CdiContainerFactory newService) {
        if (oldService == null) {
            CdiContainerFactory factory = replaceableService.getService();
            factory.setExtensionBundles(extensionObserver.getExtensionBundles());
            BundleURLScanner scanner = new BundleURLScanner("META-INF", "beans.xml", false);
            beanBundleWatcher = new BundleWatcher<URL>(bc, scanner, new BeanBundleObserver(factory));
            beanBundleWatcher.start();
        }
    }
}
