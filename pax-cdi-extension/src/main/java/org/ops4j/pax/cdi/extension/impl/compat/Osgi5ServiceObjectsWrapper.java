/*
 * Copyright 2014 Harald Wellmann.
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
package org.ops4j.pax.cdi.extension.impl.compat;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Implements {@link ServiceObjectsWrapper} for OSGi 4.3 and 5.0. The implementation simply ignores
 * the prototype scope and only handles singleton and bundle scope via the wrapped bundle context.
 *
 * @param <S>
 *            OSGi service type
 *
 * @author Harald Wellmann
 */
public class Osgi5ServiceObjectsWrapper<S> implements ServiceObjectsWrapper<S> {

    private BundleContext bundleContext;
    private ServiceReference<S> serviceReference;

    @Override
    public S getService() {
        return bundleContext.getService(serviceReference);
    }

    @Override
    public void ungetService(S service) {
        bundleContext.ungetService(serviceReference);
    }

    @Override
    public ServiceReference<S> getServiceReference() {
        return serviceReference;
    }

    @Override
    public void init(BundleContext bc, ServiceReference<S> serviceRef) {
        this.bundleContext = bc;
        this.serviceReference = serviceRef;
    }
}
