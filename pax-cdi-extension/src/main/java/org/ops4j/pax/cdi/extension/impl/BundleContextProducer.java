/*
 * Copyright 2013 Harald Wellmann
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

package org.ops4j.pax.cdi.extension.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.ops4j.pax.cdi.extension.impl.component.ComponentRegistry;
import org.ops4j.pax.cdi.extension.impl.context.SingletonScopeContext;
import org.ops4j.pax.cdi.spi.BeanBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Produces the {@link BundleContext} of the current bean bundle as managed bean.
 * 
 * @author Harald Wellmann
 * 
 */
@ApplicationScoped
public class BundleContextProducer {

    private BundleContext bundleContext;
    
    @Inject
    private OsgiExtension extension;

    @Produces
    public BundleContext bundleContext() {
        if (bundleContext == null) {
            Bundle bundle = BeanBundles.getBundle(Thread.currentThread().getContextClassLoader());
            bundleContext = bundle.getBundleContext();
        }
        return bundleContext;
    }
    
    @Produces 
    public ComponentRegistry componentRegistry() {
        return extension.getComponentRegistry();
    }

    @Produces 
    public SingletonScopeContext serviceContext() {
        return extension.getServiceContext();
    }
}
