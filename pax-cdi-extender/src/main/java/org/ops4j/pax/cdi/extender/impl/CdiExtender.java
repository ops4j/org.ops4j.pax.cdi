/*
 * Copyright 2013 Guillaume Nodet, Harald Wellmann
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

import static org.ops4j.pax.cdi.api.Constants.EXTENDER_CAPABILITY;
import static org.ops4j.pax.cdi.spi.BeanBundles.findExtensions;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.CDIProvider;

import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.cdi.spi.CdiContainerListener;
import org.ops4j.pax.cdi.spi.CdiContainerType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = { })
public class CdiExtender implements BundleTrackerCustomizer<CdiContainerWrapper> {

    private static Logger log = LoggerFactory.getLogger(CdiExtender.class);

    private BundleContext context;
    private BundleTracker<CdiContainerWrapper> bundleWatcher;
    private CdiContainerFactory factory;

    private CdiContainerListener webAdapter;

    private CDIProvider cdiProvider;

    private Map<Long, Bundle> webBundles = new HashMap<Long, Bundle>();

    @Activate
    public void activate(BundleContext ctx) {
        this.context = ctx;
        if (webAdapter != null) {
            handleWebBundles();
        }

        CDI.setCDIProvider(cdiProvider);

        log.info("starting CDI extender {}", context.getBundle().getSymbolicName());
        this.bundleWatcher = new BundleTracker<CdiContainerWrapper>(context, Bundle.ACTIVE, this);
        bundleWatcher.open();

    }

    @Deactivate
    public void deactivate(BundleContext ctx) {
        BundleCdi.dispose();

        log.info("stopping CDI extender {}", context.getBundle().getSymbolicName());
        bundleWatcher.close();
    }

    @Override
    public CdiContainerWrapper addingBundle(final Bundle bundle, BundleEvent event) {
        boolean wired = false;
        List<BundleWire> wires = bundle.adapt(BundleWiring.class).getRequiredWires(
            EXTENDER_CAPABILITY);
        if (wires != null) {
            for (BundleWire wire : wires) {
                if (wire.getProviderWiring().getBundle() == context.getBundle()) {
                    wired = true;
                    break;
                }
            }
        }
        if (wired) {
            log.debug("found bean bundle: {}", bundle.getSymbolicName());
            return createContainer(bundle);
        }
        else {
            log.trace("not a bean bundle: {}", bundle.getSymbolicName());
            return null;
        }
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, CdiContainerWrapper object) {
        // We don't care about state changes
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, CdiContainerWrapper wrapper) {
        CdiContainer container = wrapper.getCdiContainer();
        if (container != null) {
            synchronized (container) {
                container.stop();
            }
        }
        factory.removeContainer(bundle);
    }

    private CdiContainerWrapper createContainer(Bundle bundle) {
        // check if this is a web bundle
        Dictionary<String, String> headers = bundle.getHeaders();
        String contextPath = headers.get("Web-ContextPath");
        CdiContainerType containerType = (contextPath == null) ? CdiContainerType.STANDALONE
            : CdiContainerType.WEB;

        CdiContainerWrapper wrapper = new CdiContainerWrapper(bundle);
        CdiContainer container = null;
        if (containerType == CdiContainerType.WEB) {
            if (webAdapter == null) {
                webBundles.put(bundle.getBundleId(), bundle);
            }
            else {
                container = doCreateContainer(bundle, containerType);
            }
        }
        else {
            // create container, but do not start it
            // Web containers will be started later when the servlet context is available.
            // Standalone containers are started right now.
            container = doCreateContainer(bundle, containerType);
            container.start(null);
        }
        wrapper.setCdiContainer(container);
        return wrapper;
    }

    private CdiContainer doCreateContainer(Bundle bundle, CdiContainerType containerType) {
        // Find extensions
        Set<Bundle> extensions = new HashSet<Bundle>();
        findExtensions(bundle, extensions);

        log.info("creating CDI container for bean bundle {} with extension bundles {}", bundle,
            extensions);
        return factory.createContainer(bundle, extensions, containerType);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, target = "(type=web)")
    public void setWebAdapter(CdiContainerListener listener) {
        this.webAdapter = listener;
        if (context != null) {
            handleWebBundles();
        }
    }

    private void handleWebBundles() {
        factory.addListener(webAdapter);
        for (Bundle bundle : webBundles.values()) {
            doCreateContainer(bundle, CdiContainerType.WEB);
        }
        webBundles.clear();
    }

    public void unsetWebAdapter(CdiContainerListener listener) {
        if (factory != null) {
            factory.removeListener(listener);
        }
        this.webAdapter = null;
    }

    @Reference
    public void setCdiContainerFactory(CdiContainerFactory cdiContainerFactory) {
        this.factory = cdiContainerFactory;
    }

    /**
     * @param cdiProvider
     *            the cdiProvider to set
     */
    @Reference
    public void setCdiProvider(CDIProvider cdiProvider) {
        this.cdiProvider = cdiProvider;
    }

}
