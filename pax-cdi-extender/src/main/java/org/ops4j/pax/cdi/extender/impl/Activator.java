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
package org.ops4j.pax.cdi.extender.impl;

import java.net.URL;

import org.ops4j.pax.cdi.api.Constants;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.swissbox.extender.BundleManifestScanner;
import org.ops4j.pax.swissbox.extender.BundleURLScanner;
import org.ops4j.pax.swissbox.extender.ManifestEntry;
import org.ops4j.pax.swissbox.extender.RegexKeyManifestFilter;
import org.ops4j.pax.swissbox.extender.SynchronousBundleWatcher;
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
public class Activator implements BundleActivator {

    private static Logger log = LoggerFactory.getLogger(Activator.class);

    /** Bundle watcher for bean bundles. */
    private SynchronousBundleWatcher<ManifestEntry> beanBundleWatcher;

    /** Bundle watcher for CDI extension bundles. */
    private SynchronousBundleWatcher<URL> extensionWatcher;

    private CdiExtensionObserver extensionObserver;

    private BeanBundleObserver beanBundleObserver;

    private CdiExtender cdiExtender;

    @SuppressWarnings("unchecked")
    public void start(BundleContext bc) throws Exception {
        log.debug("starting bundle {}", bc.getBundle().getSymbolicName());

        BundleURLScanner extensionScanner = new BundleURLScanner("META-INF/services",
            "javax.enterprise.inject.spi.Extension", false);
        extensionObserver = new CdiExtensionObserver();

        cdiExtender = new CdiExtender(bc, extensionObserver);
        beanBundleObserver = new BeanBundleObserver(cdiExtender);
        extensionWatcher = new SynchronousBundleWatcher<URL>(bc, extensionScanner, extensionObserver);

        RegexKeyManifestFilter filter = new RegexKeyManifestFilter(Constants.MANAGED_BEANS_KEY);
        BundleManifestScanner beanBundleScanner = new BundleManifestScanner(filter);
        
        //BundleURLScanner beanBundleScanner = new BundleURLScanner("META-INF", "beans.xml", false);
        beanBundleWatcher = new SynchronousBundleWatcher<ManifestEntry>(bc, beanBundleScanner, beanBundleObserver);

        extensionWatcher.start();
        beanBundleWatcher.start();
    }

    public void stop(BundleContext context) throws Exception {
        log.debug("stopping bundle {}", context.getBundle().getSymbolicName());
        beanBundleWatcher.stop();
        extensionWatcher.stop();
        cdiExtender.stop();
    }
}
