/*
 * Copyright 2013 Guillaume Nodet
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

import org.ops4j.pax.cdi.api.Constants;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.cdi.spi.CdiContainerType;
import org.ops4j.pax.swissbox.tracker.ReplaceableService;
import org.ops4j.pax.swissbox.tracker.ReplaceableServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TODO: the SynchronousBundleWatcher uses the STOPPED event instead of STOPPING
 */
public class CdiExtender implements BundleActivator,
                                    ReplaceableServiceListener<CdiContainerFactory>,
                                    BundleTrackerCustomizer<CdiContainer>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(CdiExtender.class);

    private BundleContext context;
    private ReplaceableService<CdiContainerFactory> factoryTracker;
    private BundleTracker<CdiContainer> bundleWatcher;
    private CdiContainerFactory factory;

    @Override
    public void start(BundleContext context) throws Exception {
        LOGGER.info("Starting CDI extender {}", context.getBundle().getSymbolicName());
        this.context = context;
        this.factoryTracker = new ReplaceableService<CdiContainerFactory>(context, CdiContainerFactory.class, this);
        this.bundleWatcher = new BundleTracker<CdiContainer>(context, Bundle.ACTIVE, this);
        this.factoryTracker.start();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LOGGER.info("Stopping CDI extender {}", context.getBundle().getSymbolicName());
        this.factoryTracker.stop();
    }

    @Override
    public synchronized void serviceChanged(CdiContainerFactory oldService, CdiContainerFactory newService) {
        if (oldService != null) {
            this.bundleWatcher.close();
        }
        this.factory = newService;
        if (newService != null) {
            this.bundleWatcher.open();
        }
    }

    @Override
    public CdiContainer addingBundle(final Bundle bundle, BundleEvent event) {
        boolean wired = false;
        List<BundleWire> wires = bundle.adapt(BundleWiring.class).getRequiredWires("osgi.extender");
        if (wires != null) {
            for (BundleWire wire : wires) {
                if (wire.getProviderWiring().getBundle() == context.getBundle()) {
                    wired = true;
                    break;
                }
            }
        }
        if (wired) {
            try {
                LOGGER.debug("Found CDI application in bundle {}", bundle.getSymbolicName());
                return createContainer(bundle);
            } catch (Exception e) {
                LOGGER.error("Error creating CDI container for bundle " + bundle.toString(), e);
            }
        } else {
            LOGGER.debug("No CDI application found in bundle {}", bundle.getSymbolicName());
        }
        return null;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, CdiContainer object) {
        // We don't care about state changes
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, CdiContainer container) {
        synchronized (container) {
            container.stop();
        }
        this.factory.removeContainer(bundle);
    }

    private CdiContainer createContainer(Bundle bundle) {
        CdiContainerFactory cf = factory;
        if (cf != null) {
            // Find extensions
            Set<Bundle> extensions = new HashSet<Bundle>();
            findExtensions(bundle, extensions);
            // Find beans xml
            final List<URL> beansXml = new ArrayList<URL>();
            scan(bundle, beansXml);
            for (Bundle ext : extensions) {
                scan(ext, beansXml);
            }

            // check if this is a web bundle
            Dictionary<String, String> headers = bundle.getHeaders();
            String contextPath = headers.get("Web-ContextPath");
            CdiContainerType containerType = (contextPath == null) ? CdiContainerType.STANDALONE : CdiContainerType.WEB;
            // create container, but do not start it
            LOGGER.info("Creating CDI container for bundle {} with beans xml {} and extensions {}", new Object[] { bundle, beansXml, extensions });
            final CdiContainer container = cf.createContainer(bundle, beansXml, extensions, containerType);
            // Web containers will be started later when the servlet context is available.
            // Standalone containers are started right now.
            if (containerType == CdiContainerType.STANDALONE) {
                container.start(null);
            }
            return container;
        }
        return null;
    }

    private void findExtensions(Bundle bundle, Set<Bundle> extensions) {
        List<BundleWire> wires = bundle.adapt(BundleWiring.class).getRequiredWires("org.ops4j.pax.cdi.extension");
        if (wires != null) {
            for (BundleWire wire : wires) {
                Bundle b = wire.getProviderWiring().getBundle();
                extensions.add(b);
                findExtensions(b, extensions);
            }
        }
    }

    private void scan(Bundle bundle, List<URL> pathList) {
        LOGGER.debug("Scanning bundle {} for CDI application", bundle.getSymbolicName());
        String header = bundle.getHeaders().get(Constants.MANAGED_BEANS_KEY);
        if (header == null) {
            if (bundle.findEntries("META-INF/", "beans.xml", false) != null) {
                header = "META-INF/beans.xml";
            }
        }
        Parser.Clause[] paths = Parser.parseHeader(header);
        for (Parser.Clause path : paths) {
            String name = path.getName();
            if (name.endsWith("/")) {
                Enumeration<URL> e = bundle.findEntries(name, "*.xml", false);
                while (e != null && e.hasMoreElements()) {
                    URL u = e.nextElement();
                    pathList.add(u);
                }
            } else {
                String baseName;
                String filePattern;
                int pos = name.lastIndexOf('/');
                if (pos < 0) {
                    baseName = "/";
                    filePattern = name;
                } else {
                    baseName = name.substring(0, pos + 1);
                    filePattern = name.substring(pos + 1);
                }
                if (filePattern.contains("*")) {
                    Enumeration<URL> e = bundle.findEntries(baseName, filePattern, false);
                    while (e != null && e.hasMoreElements()) {
                        URL u = e.nextElement();
                        pathList.add(u);
                    }
                } else {
                    URL url = bundle.getEntry(name);
                    if (url == null) {
                        throw new IllegalArgumentException("Unable to find CDI configuration file for " + path);
                    }
                    pathList.add(url);
                }
            }
        }
    }

}
