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
package org.ops4j.pax.cdi.undertow.openwebbeans.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.SingletonService;
import org.apache.webbeans.util.Asserts;
import org.ops4j.pax.cdi.spi.util.Exceptions;
import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton service which determines the current WebBeansContext based on the extended bundle
 * class loader.
 *
 * @author Harald Wellmann
 */
public class BundleSingletonService implements SingletonService<WebBeansContext> {

    private static Logger log = LoggerFactory.getLogger(BundleSingletonService.class);

    /**
     * Maps bundle IDs to contexts.
     */
    private final Map<Long, WebBeansContext> singletonMap = new HashMap<Long, WebBeansContext>();

    @Override
    public WebBeansContext get(Object key) {
        Bundle bundle = toBundle(key);
        long bundleId = bundle.getBundleId();
        synchronized (singletonMap) {
            WebBeansContext webBeansContext = singletonMap.get(bundleId);

            if (webBeansContext == null) {
                Properties props = new Properties();
                Map<Class<?>, Object> initialServices = new HashMap<Class<?>, Object>();

                String resource = "/META-INF/openwebbeans/standalone.properties";
                if (bundle.getHeaders().get("Web-ContextPath") != null) {
                    resource = "/META-INF/openwebbeans/wab.properties";
                }

                try {
                    props.load(getClass().getResourceAsStream(resource));
                }
                catch (IOException exc) {
                    throw Exceptions.unchecked(exc);
                }

                webBeansContext = new WebBeansContext(initialServices, props);
                singletonMap.put(bundleId, webBeansContext);
            }

            return webBeansContext;

        }
    }

    /**
     * Clear all deployment instances when the application is undeployed.
     *
     * @param classLoader
     *            of the deployment
     */
    public void clearInstances(ClassLoader classLoader) {
        Asserts.assertNotNull(classLoader, "classloader is null");
        Bundle bundle = toBundle(classLoader);
        synchronized (singletonMap) {
            singletonMap.remove(bundle.getBundleId());
        }
    }

    @Override
    public void clear(Object classLoader) {
        clearInstances((ClassLoader) classLoader);
    }

    /**
     * Assumes that the key is a bundle classloader and returns the corresponding bundle.
     *
     * @param key
     * @return bundle
     */
    private Bundle toBundle(Object key) {
        // workaround for weird context class loader from Pax Web 3.1.1
        if (key instanceof BundleClassLoader) {
            BundleClassLoader bcl = (BundleClassLoader) key;
            if (bcl.getParent() instanceof BundleReference) {
                return BundleReference.class.cast(bcl.getParent()).getBundle();
            }
        }
        if (key instanceof BundleReference) {
            return BundleReference.class.cast(key).getBundle();
        }

        log.error("classloader {} does not implement BundleReference", key);
        return null;
    }
}
