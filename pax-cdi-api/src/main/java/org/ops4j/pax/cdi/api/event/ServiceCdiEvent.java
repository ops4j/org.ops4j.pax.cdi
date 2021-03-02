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
package org.ops4j.pax.cdi.api.event;

import org.osgi.framework.ServiceReference;

/**
 * CDI event fired when a service is added or removed.
 *
 * @param <T>
 *            service type
 *
 * @author Harald Wellmann
 *
 */
public class ServiceCdiEvent<T> {

    private ServiceReference<T> reference;
    private T service;

    /**
     * Creates an event for the given service and its reference.
     *
     * @param reference
     *            service reference
     * @param service
     *            associated service
     */
    public ServiceCdiEvent(ServiceReference<T> reference, T service) {
        this.reference = reference;
        this.service = service;
    }

    /**
     * Gets the service reference.
     *
     * @return the reference
     */
    public ServiceReference<T> getReference() {
        return reference;
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    public T getService() {
        return service;
    }
}
