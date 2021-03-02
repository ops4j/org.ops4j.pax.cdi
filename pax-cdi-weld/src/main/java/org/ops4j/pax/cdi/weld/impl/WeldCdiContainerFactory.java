/*
 * Copyright 2021 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.weld.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.weld.bootstrap.api.SingletonProvider;
import org.jboss.weld.bootstrap.api.helpers.RegistrySingletonProvider;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link CdiContainerFactory} implementation based on Weld.
 *
 * @author Harald Wellmann
 *
 */
public class WeldCdiContainerFactory implements CdiContainerFactory {

    private Logger log = LoggerFactory.getLogger(WeldCdiContainerFactory.class);

    private Map<Long, CdiContainer> containers = new HashMap<Long, CdiContainer>();
    private BundleContext bundleContext;

    /**
     * Called by the OSGi framework when this bundle is started. Sets the singleton provider.
     *
     * @param bc
     *            bundle context of this bundle
     */
    public void activate(BundleContext bc) {
        this.bundleContext = bc;
        SingletonProvider.initialize(new RegistrySingletonProvider());
    }

    /**
     * Called by the OSGi framework when this bundle is stopped. Resets the singleton provider.
     */
    public void deactivate() {
        SingletonProvider.reset();
    }

    @Override
    public String getProviderName() {
        return "Weld";
    }

    @Override
    public CdiContainer createContainer(Bundle bundle, Collection<Bundle> extensions) {
        WeldCdiContainer container = new WeldCdiContainer(bundleContext.getBundle(),
            bundle, extensions);
        containers.put(bundle.getBundleId(), container);
        log.debug("Weld Container created");
        return container;
    }

    @Override
    public CdiContainer getContainer(Bundle bundle) {
        return containers.get(bundle.getBundleId());
    }

    @Override
    public Collection<CdiContainer> getContainers() {
        return Collections.unmodifiableCollection(containers.values());
    }

    @Override
    public void removeContainer(Bundle bundle) {
        containers.remove(bundle.getBundleId());
    }

}
