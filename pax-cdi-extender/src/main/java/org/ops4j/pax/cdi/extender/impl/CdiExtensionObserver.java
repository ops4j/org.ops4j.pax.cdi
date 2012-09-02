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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.spi.Extension;

import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.ops4j.pax.swissbox.extender.BundleObserver;
import org.ops4j.spi.SafeServiceLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observes CDI extension bundles, i.e. all bundles representing a CDI portable extension as
 * indicated by a {@code META-INF/services/javax.enterprise.inject.spi.Extension} resource.
 * 
 * @author Harald Wellmann
 * 
 */
public class CdiExtensionObserver implements BundleObserver<URL> {

    private static Logger log = LoggerFactory.getLogger(CdiExtensionObserver.class);

    /**
     * Maps bundle IDs to CDI extension bundles.
     */
    private Map<Long, Bundle> extensionBundles = new ConcurrentHashMap<Long, Bundle>();

    @Override
    public void addingEntries(Bundle bundle, List<URL> entries) {
        log.info("found CDI extension in bundle {}_{}", bundle.getSymbolicName(),
            bundle.getVersion());

        extensionBundles.put(bundle.getBundleId(), bundle);
        SafeServiceLoader serviceLoader = new SafeServiceLoader(new BundleClassLoader(bundle));
        List<Extension> extensions = serviceLoader.load(Extension.class.getName());
        for (Extension extension : extensions) {
            BundleContext bc = bundle.getBundleContext();
            bc.registerService(Extension.class.getName(), extension, null);            
        }
    }

    @Override
    public void removingEntries(Bundle bundle, List<URL> entries) {
        extensionBundles.remove(bundle.getBundleId());
    }

    public Collection<Bundle> getExtensionBundles() {
        return extensionBundles.values();
    }
}
