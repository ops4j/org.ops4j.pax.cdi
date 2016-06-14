/*
 * Copyright 2014 Harald Wellmann
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

import static org.ops4j.pax.cdi.extension.impl.compat.OsgiScopeUtils.createServiceObjectsWrapper;

import java.lang.annotation.Annotation;

import javax.enterprise.event.Event;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;

import org.ops4j.pax.cdi.api.event.ServiceCdiEvent;
import org.ops4j.pax.cdi.extension.impl.compat.ServiceObjectsWrapper;
import org.ops4j.pax.cdi.extension.impl.util.ParameterizedTypeLiteral;
import org.ops4j.pax.cdi.extension.impl.util.ServiceAddedLiteral;
import org.ops4j.pax.cdi.extension.impl.util.ServiceRemovedLiteral;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * Maps OSGi service events to CDI events. Fires events qualified with {@code ServiceAdded} or
 * {@code ServiceRemoved}.
 *
 * @author Harald Wellmann
 *
 */
public class ServiceEventBridge implements ServiceListener {

    @Inject
    private BundleContext bundleContext;

    @Inject
    private Event<Object> event;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void serviceChanged(ServiceEvent serviceEvent) {
        Annotation qualifier = toQualifier(serviceEvent);
        if (qualifier == null) {
            return;
        }

        ServiceReference serviceReference = serviceEvent.getServiceReference();
        ServiceObjectsWrapper serviceObjects =
            createServiceObjectsWrapper(bundleContext, serviceReference);
        Object service = serviceObjects.getService();

        if (service != null) {
            try {
                Class klass = service.getClass();
                event.select(klass, qualifier).fire(service);

                TypeLiteral literal = new ParameterizedTypeLiteral(ServiceCdiEvent.class, klass);
                ServiceCdiEvent cdiEvent = new ServiceCdiEvent(serviceReference, service);
                event.select(literal, qualifier).fire(cdiEvent);
            }
            finally {
                serviceObjects.ungetService(service);
            }
        }
    }

    private Annotation toQualifier(ServiceEvent serviceEvent) {
        switch (serviceEvent.getType()) {
            case ServiceEvent.REGISTERED:
                return new ServiceAddedLiteral();

            case ServiceEvent.UNREGISTERING:
                return new ServiceRemovedLiteral();

            default:
                return null;
        }
    }

    /**
     * Starts the service event bridge.
     */
    public void start() {
        bundleContext.addServiceListener(this);
    }

    /**
     * Stops the service event bridge.
     */
    public void stop() {
        bundleContext.removeServiceListener(this);
    }
}
