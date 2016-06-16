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
import org.ops4j.pax.cdi.extension.impl.component2.BundleContextHolder;
import org.osgi.framework.BundleContext;

/**
 * Produces the {@link BundleContext} of the current bean bundle and some other managed beans.
 *
 * @author Harald Wellmann
 *
 */
@ApplicationScoped
class BundleContextProducer {

    @Inject
    private OsgiExtension extension;

    /**
     * Produces the bundle context for the current bean bundle.
     * @return bundle context
     */
    @Produces
    BundleContext getBundleContext() {
        return BundleContextHolder.getBundleContext();
    }

    /**
     * Produces the component registry for the current bean bundle.
     * @return component registry
     */
    @Produces
    ComponentRegistry getComponentRegistry() {
        return extension.getComponentRegistry();
    }
}
