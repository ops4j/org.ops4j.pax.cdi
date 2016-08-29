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
 *
 * Derived from org.apache.webbeans.corespi.DefaultSingletonService.
 */
package org.ops4j.pax.cdi.web.openwebbeans.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.SingletonService;
import org.apache.webbeans.util.Asserts;
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
                    throw unchecked(exc);
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
        if (key instanceof BundleReference) {
            return BundleReference.class.cast(key).getBundle();
        }

        log.error("classloader {} does not implement BundleReference", key);
        return null;
    }

    private static RuntimeException unchecked(Throwable exc) {
        BundleSingletonService.<RuntimeException>adapt(exc);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Exception> void adapt(Throwable exc) throws T {
        throw (T) exc;
    }

}
