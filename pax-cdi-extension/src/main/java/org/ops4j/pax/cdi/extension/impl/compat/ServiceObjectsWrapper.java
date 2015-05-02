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
 * Wrapper for the {@code org.osgi.framework.ServiceObjects} interface introduced in OSGi Core 6.0.
 * Used for backward compatibility with OSGI 4.3.
 *
 * @author Harald Wellmann
 *
 * @param <S>
 *            service type
 */
public interface ServiceObjectsWrapper<S> {

    /**
     * Initializes this wrapper for the given bundle context and service reference.
     *
     * @param bc
     *            bundle context
     * @param serviceReference
     *            service reference
     */
    void init(BundleContext bc, ServiceReference<S> serviceReference);

    /**
     * Gets a service for the wrapped reference.
     *
     * @return service service instance
     */
    S getService();

    /**
     * Ungets a service instance for the wrapped reference.
     *
     * @param service
     *            service instance
     */
    void ungetService(S service);

    /**
     * Gets the wrapped service reference.
     *
     * @return service reference.
     */
    ServiceReference<S> getServiceReference();
}
