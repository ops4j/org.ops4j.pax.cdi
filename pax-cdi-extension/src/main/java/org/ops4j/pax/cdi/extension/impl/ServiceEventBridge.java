/*
 * Copyright 2014 Harald Wellmann
 * Copyright 2016 Guillaume Nodet
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

import java.lang.annotation.Annotation;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ops4j.pax.cdi.api.event.ServiceCdiEvent;
import org.ops4j.pax.cdi.extension.impl.support.Filters;
import org.ops4j.pax.cdi.extension.impl.util.ParameterizedTypeLiteral;
import org.ops4j.pax.cdi.extension.impl.util.ServiceAddedLiteral;
import org.ops4j.pax.cdi.extension.impl.util.ServiceRemovedLiteral;
import org.osgi.framework.*;

/**
 * Maps OSGi service events to CDI events. Fires events qualified with {@code ServiceAdded} or
 * {@code ServiceRemoved}.
 *
 * @author Harald Wellmann
 * @author Guillaume Nodet
 *
 */
@ApplicationScoped
public class ServiceEventBridge implements ServiceListener {

    @Inject
    private BundleContext bundleContext;

    @Inject
    private Event<Object> event;

    @Inject
    private OsgiExtension2 extension;

    private String filter;
    private Map<Annotation, Filter> filters;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void serviceChanged(ServiceEvent serviceEvent) {
        Annotation qualifier = toQualifier(serviceEvent);
        if (qualifier == null) {
            return;
        }

        ServiceReference serviceReference = serviceEvent.getServiceReference();
        ServiceObjects serviceObjects =
                bundleContext.getServiceObjects(serviceReference);

        Object service = serviceObjects.getService();

        try {
            Class klass = service.getClass();
            Event<Object> event = this.event;
            for (Map.Entry<Annotation, Filter> entry : filters.entrySet()) {
                if (entry.getValue().match(serviceReference)) {
                    event = event.select(entry.getKey());
                }
            }
            event.select(klass, qualifier).fire(service);

            TypeLiteral literal = new ParameterizedTypeLiteral(ServiceCdiEvent.class, klass);
            ServiceCdiEvent cdiEvent = new ServiceCdiEvent(serviceReference, service);
            event.select(literal, qualifier).fire(cdiEvent);
        }
        finally {
            serviceObjects.ungetService(service);
        }
    }

    @PostConstruct
    public void init() {
        filter = Filters.or(extension.getObservedFilters());
        if (filter != null) {
            try {
                filters = new HashMap<>();
                for (Annotation annotation : extension.getObservedQualifiers()) {
                    String flt = Filters.getFilter(Collections.singleton(annotation));
                    if (flt != null) {
                        filters.put(annotation, bundleContext.createFilter(flt));
                    }
                }
                bundleContext.addServiceListener(this, filter);
            } catch (InvalidSyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @PreDestroy
    public void destroy() {
        if (filter != null) {
            bundleContext.removeServiceListener(this);
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

    // Force the instantation of this bean
    public void applicationScopeInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
    }

}
