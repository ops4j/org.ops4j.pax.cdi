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
import java.util.List;

import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.swissbox.extender.BundleObserver;
import org.osgi.framework.Bundle;
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

    private CdiExtender extender;

    public BeanBundleObserver(CdiExtender extender) {
        this.extender = extender;
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
        extender.createContainer(bundle);
    }

    @Override
    public void removingEntries(Bundle bundle, List<URL> entries) {
        log.info("stopping CDI container of bean bundle {}_{}", bundle.getSymbolicName(),
            bundle.getVersion());
        extender.destroyContainer(bundle);
    }
}
