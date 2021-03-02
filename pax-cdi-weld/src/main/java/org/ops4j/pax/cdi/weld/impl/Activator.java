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

import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;

/**
 * Activator for Weld CDI Adapter.
 *
 * @author Harald Wellmann
 *
 */
public class Activator implements BundleActivator {

    private ServiceRegistration<WeavingHook> registration;
    private ServiceRegistration<CdiContainerFactory> factoryRegistration;

    private WeldCdiContainerFactory factory = new WeldCdiContainerFactory();

    /**
     * Starts this bundle and registers a weaving hook for Weld proxies.
     */
    @Override
    public void start(BundleContext context) throws Exception {
        ProxyWeavingHook weavingHook = new ProxyWeavingHook();
        registration = context.registerService(WeavingHook.class, weavingHook, null);
        factory.activate(context);
        factoryRegistration = context.registerService(CdiContainerFactory.class, factory, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        registration.unregister();
        factoryRegistration.unregister();
        factory.deactivate();
    }
}
