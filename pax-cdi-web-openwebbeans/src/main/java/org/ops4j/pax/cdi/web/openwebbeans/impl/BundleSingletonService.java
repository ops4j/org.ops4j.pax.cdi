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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.SingletonService;
import org.apache.webbeans.util.Asserts;
import org.ops4j.lang.Ops4jException;
import org.osgi.framework.Bundle;

public class BundleSingletonService implements SingletonService<WebBeansContext> {

    /**
     * Maps bundle IDs to contexts.
     */
    private final Map<Long, WebBeansContext> singletonMap = new HashMap<Long, WebBeansContext>();

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
                    initialServices.put(ContainerLifecycle.class, new WabContainerLifecycle());
                    initialServices.put(ContextsService.class, new WabContextsService());
                }

                try {
                    props.load(getClass().getResourceAsStream(resource));
                }
                catch (IOException exc) {
                    throw new Ops4jException(exc);
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
     * Assumes that the key is a bundle classloader and returns the corresponding bundle. The ugly
     * reflection hack is due to the fact that Pax Web 3.0.0.M1 embeds pax-swissbox-core instead of
     * importing it, whereas Pax CDI imports the class, resulting in a mismatch.
     * 
     * @param key
     * @return bundle
     */
    private Bundle toBundle(Object key) {
        try {
            Method method = key.getClass().getMethod("getBundle");
            Bundle bundle = (Bundle) method.invoke(key);
            return bundle;
        }
        catch (SecurityException exc) {
            wrapException(exc, key);
        }
        catch (NoSuchMethodException exc) {
            wrapException(exc, key);
        }
        catch (IllegalArgumentException exc) {
            wrapException(exc, key);
        }
        catch (IllegalAccessException exc) {
            wrapException(exc, key);
        }
        catch (InvocationTargetException exc) {
            wrapException(exc, key);
        }
        return null;
    }

    private void wrapException(Exception exc, Object key) {
        throw new IllegalArgumentException("key is " + key.getClass().getName()
            + " but must be BundleClassLoader for using BundleSingletonService");
    }
}
