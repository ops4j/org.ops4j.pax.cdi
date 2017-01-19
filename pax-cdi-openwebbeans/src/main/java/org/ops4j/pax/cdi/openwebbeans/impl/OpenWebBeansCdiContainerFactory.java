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
package org.ops4j.pax.cdi.openwebbeans.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.webbeans.config.WebBeansContext;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * {@link CdiContainerFactory} implementation based on Apache OpenWebBeans.
 *
 * @author Harald Wellmann
 *
 */
public class OpenWebBeansCdiContainerFactory implements CdiContainerFactory {

    private Map<Long, CdiContainer> containers = new HashMap<Long, CdiContainer>();
    private BundleContext bundleContext;

    public OpenWebBeansCdiContainerFactory() {
    }

    /**
     * Called by the service component runtime when this component gets activated.
     *
     * @param context
     *            bundle context
     */
    public void activate(BundleContext context) {
        this.bundleContext = context;
    }

    @Override
    public String getProviderName() {
        return WebBeansContext.class.getName();
    }

    @Override
    public CdiContainer createContainer(Bundle bundle, Collection<Bundle> extensions) {
        Bundle ownBundle = bundleContext.getBundle();
        OpenWebBeansCdiContainer container = new OpenWebBeansCdiContainer(ownBundle,
            bundle, extensions);
        containers.put(bundle.getBundleId(), container);
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
