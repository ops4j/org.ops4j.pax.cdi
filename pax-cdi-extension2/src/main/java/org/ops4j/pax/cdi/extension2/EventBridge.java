/*
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
package org.ops4j.pax.cdi.extension2;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ops4j.pax.cdi.api2.event.ServiceAdded;
import org.ops4j.pax.cdi.api2.event.ServiceCdiEvent;
import org.ops4j.pax.cdi.api2.event.ServiceRemoved;
import org.ops4j.pax.cdi.extension2.support.Filters;
import org.ops4j.pax.cdi.extension2.support.ParameterizedTypeLiteral;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

@ApplicationScoped
public class EventBridge implements ServiceListener {

    public static final AnnotationLiteral<ServiceAdded> ADDED_ANNOTATION_LITERAL = new AnnotationLiteral<ServiceAdded>() { };

    public static final AnnotationLiteral<ServiceRemoved> REMOVED_ANNOTATION_LITERAL = new AnnotationLiteral<ServiceRemoved>() { };

    @Inject
    private BundleContext bundleContext;

    @Inject
    private Event<Object> event;

    @Inject
    private OsgiExtension extension;

    private String filter;
    private Map<Annotation, Filter> filters;


    @Override
    public void serviceChanged(ServiceEvent event) {
        Annotation qualifier = toQualifier(event);
        if (qualifier == null) {
            return;
        }
        fire(qualifier, event.getServiceReference());
    }

    protected <T> void fire(Annotation qualifier, ServiceReference<T> serviceReference) {
        ServiceObjects<T> serviceObjects = bundleContext.getServiceObjects(serviceReference);
        T service = serviceObjects.getService();

        try {
            Event<Object> event = this.event;
            for (Map.Entry<Annotation, Filter> entry : filters.entrySet()) {
                if (entry.getValue().match(serviceReference)) {
                    event = event.select(entry.getKey());
                }
            }

            @SuppressWarnings("unchecked")
            Class<? super T> klass = (Class) service.getClass();
            event.select(klass, qualifier).fire(service);

            @SuppressWarnings("unchecked")
            TypeLiteral<ServiceCdiEvent<T>> literal = (TypeLiteral) new ParameterizedTypeLiteral(ServiceCdiEvent.class, klass);
            ServiceCdiEvent<T> cdiEvent = new ServiceCdiEvent<>(serviceReference, service);
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
                return ADDED_ANNOTATION_LITERAL;

            case ServiceEvent.UNREGISTERING:
                return REMOVED_ANNOTATION_LITERAL;

            default:
                return null;
        }
    }

    // Force the instantation of this bean
    public void applicationScopeInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
    }

}
