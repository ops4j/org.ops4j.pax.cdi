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

import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator for OpenWebBeans {@link CdiContainerFactory} implementation.
 * 
 * @author Harald Wellmann
 * 
 */
public class Activator implements BundleActivator {

    /**
     * Starts this bundle and registers a {@link CdiContainerFactory} service.
     */
    @Override
    public void start(BundleContext context) throws Exception {
        OpenWebBeansCdiContainerFactory factory = new OpenWebBeansCdiContainerFactory(
            context.getBundle());
        context.registerService(CdiContainerFactory.class.getName(), factory, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
