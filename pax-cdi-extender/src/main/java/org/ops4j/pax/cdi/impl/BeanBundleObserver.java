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

import static org.ops4j.pax.swissbox.core.ContextClassLoaderUtils.doWithClassLoader;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;

import javax.enterprise.inject.spi.BeanManager;

import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.cdi.api.BeanBundle;
import org.ops4j.pax.cdi.api.ContainerInitialized;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.swissbox.extender.BundleObserver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observes bean bundles and manages the lifecycle of the associated {@link CdiContainer}s.
 * 
 * @author Harald Wellmann
 * 
 */
public class BeanBundleObserver implements BundleObserver<URL> {

    private static Logger log = LoggerFactory.getLogger(BeanBundleObserver.class);

    /** Factory for creating CDI containers. */
    private CdiContainerFactory containerFactory;

    public BeanBundleObserver(CdiContainerFactory containerFactory) {
        this.containerFactory = containerFactory;
    }

    /**
     * Creates and starts the CDI container for a newly discovered bean bundle. The bundle state is
     * STARTED.
     * <p>
     * TODO Do we need to parse the {@code beans.xml} descriptor or is this done by the CDI
     * implementation?
     * 
     * @param bundle
     *            bean bundle to be extended with a CDI container
     * @param entries
     *            {@code beans.xml} resources in this bundle.
     */
    @Override
    public void addingEntries(final Bundle bundle, List<URL> entries) {
        log.info("discovered bean bundle {}_{}", bundle.getSymbolicName(), bundle.getVersion());

        final CdiContainer container = containerFactory.createContainer(bundle);
        container.start();

        /*
         * Start the CDI container under a suitable thread context class loader so that the CDI
         * implementation will be able to load classes from all required bundles.
         * 
         * TODO Move this to CdiContainer implementation?
         */
        try {
            doWithClassLoader(container.getContextClassLoader(),
                new Callable<ServiceRegistration<CdiContainer>>() {

                    @Override
                    public ServiceRegistration<CdiContainer> call() throws Exception {
                        // set bundle context on BeanBundle CDI bean
                        BeanBundle cdiBundle = container.getInstance().select(BeanBundle.class)
                            .get();
                        BundleContext bc = bundle.getBundleContext();
                        cdiBundle.setBundleContext(bc);

                        // fire ContainerInitialized event
                        BeanManager beanManager = container.getBeanManager();
                        beanManager.fireEvent(new ContainerInitialized());

                        // register CdiContainer service
                        Dictionary<String, Object> props = new Hashtable<String, Object>();
                        props.put("bundleId", bundle.getBundleId());
                        props.put("symbolicName", bundle.getSymbolicName());

                        return bc.registerService(CdiContainer.class, container, props);
                    }
                });
        }
        catch (Exception exc) {
            throw new Ops4jException(exc);
        }
    }

    @Override
    public void removingEntries(Bundle bundle, List<URL> entries) {
        log.info("stopping CDI container of bean bundle {}_{}", bundle.getSymbolicName(),
            bundle.getVersion());

        CdiContainer container = containerFactory.getContainer(bundle);
        container.stop();
        containerFactory.removeContainer(bundle);

        // TODO remove CdiContainer service registration
    }
}
