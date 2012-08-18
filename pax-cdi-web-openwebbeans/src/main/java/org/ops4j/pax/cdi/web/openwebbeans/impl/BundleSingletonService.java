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
import java.util.WeakHashMap;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.SingletonService;
import org.apache.webbeans.util.Asserts;
import org.apache.xbean.osgi.bundle.util.BundleClassLoader;
import org.ops4j.lang.Ops4jException;
import org.osgi.framework.Bundle;

public class BundleSingletonService implements SingletonService<WebBeansContext> {

	/**
	 * Maps bundle class loaders to contexts.
	 */
	private final Map<ClassLoader, WebBeansContext> singletonMap = new WeakHashMap<ClassLoader, WebBeansContext>();

	public WebBeansContext get(Object key) {
		ClassLoader classLoader = (ClassLoader) key;
		synchronized (singletonMap) {
			WebBeansContext webBeansContext = singletonMap.get(classLoader);

			if (webBeansContext == null) {
				Bundle bundle = assertBundleClassLoaderKey(key);
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
				singletonMap.put(classLoader, webBeansContext);
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
		synchronized (singletonMap) {
			singletonMap.remove(classLoader);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void clear(Object classLoader) {
		assertBundleClassLoaderKey(classLoader);
		clearInstances((ClassLoader) classLoader);
	}

	/**
	 * Assert that key is classloader instance.
	 * 
	 * @param key
	 *            key
	 */
	private Bundle assertBundleClassLoaderKey(Object key) {
		if (!(key instanceof BundleClassLoader)) {
			throw new IllegalArgumentException(
				"key must be BundleClassLoader for using BundleSingletonService");
		}

		BundleClassLoader cl = (BundleClassLoader) key;
		Bundle bundle = cl.getBundle();
		return bundle;
	}
}
