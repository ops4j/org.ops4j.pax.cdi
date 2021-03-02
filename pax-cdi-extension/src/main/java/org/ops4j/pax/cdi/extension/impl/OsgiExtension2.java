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
package org.ops4j.pax.cdi.extension.impl;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.ops4j.pax.cdi.api.BundleScoped;
import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.Config;
import org.ops4j.pax.cdi.api.Global;
import org.ops4j.pax.cdi.api.PrototypeScoped;
import org.ops4j.pax.cdi.api.Service;
import org.ops4j.pax.cdi.api.SingletonScoped;
import org.ops4j.pax.cdi.api.event.ServiceCdiEvent;
import org.ops4j.pax.cdi.extension.impl.component2.BundleContextHolder;
import org.ops4j.pax.cdi.extension.impl.component2.ComponentDescriptor;
import org.ops4j.pax.cdi.extension.impl.component2.ComponentRegistry;
import org.ops4j.pax.cdi.extension.impl.component2.GlobalDescriptor;
import org.ops4j.pax.cdi.extension.impl.context.BundleScopeContext;
import org.ops4j.pax.cdi.extension.impl.context.PrototypeScopeContext;
import org.ops4j.pax.cdi.extension.impl.context.SingletonScopeContext;
import org.ops4j.pax.cdi.extension.impl.osgi.Registry;
import org.ops4j.pax.cdi.extension.impl.support.DelegatingBeanAttributes;
import org.ops4j.pax.cdi.extension.impl.support.DelegatingInjectionPoint;
import org.ops4j.pax.cdi.extension.impl.support.DelegatingInjectionTarget;
import org.ops4j.pax.cdi.extension.impl.support.Filters;
import org.ops4j.pax.cdi.extension.impl.support.Types;
import org.ops4j.pax.cdi.extension.impl.util.ServiceAddedLiteral;
import org.ops4j.pax.cdi.extension.impl.util.ServiceRemovedLiteral;
import org.osgi.framework.Constants;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@ApplicationScoped
public class OsgiExtension2 implements Extension {

    private ComponentRegistry componentRegistry;
    private GlobalDescriptor global;

    private final Set<String> observedFilters = new HashSet<>();
    private final Set<Annotation> observedQualifiers = new HashSet<>();

    public OsgiExtension2() {
    }

    public ComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }

    public <T> Bean<T> globalDependency(Class<T> clazz, Set<Annotation> qualifiers) {
        return global.addGlobalInjectionPoint(clazz, qualifiers);
    }

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager manager) {
        componentRegistry = new ComponentRegistry(manager, BundleContextHolder.getBundleContext());
        global = new GlobalDescriptor(componentRegistry);

        event.addAnnotatedType(manager.createAnnotatedType(BundleEventBridge.class));
        event.addAnnotatedType(manager.createAnnotatedType(ServiceEventBridge.class));
        event.addAnnotatedType(manager.createAnnotatedType(BundleContextProducer.class));
        event.addScope(SingletonScoped.class, false, false);
    }

    public <T> void processBeanAttributes(@Observes ProcessBeanAttributes<T> event) {
        if (event.getAnnotated().isAnnotationPresent(Component.class)
                || event.getAnnotated().isAnnotationPresent(Global.class)) {
            BeanAttributes<T> attr = event.getBeanAttributes();
            Class<? extends Annotation> scope = attr.getScope();
            if (scope == SingletonScoped.class || scope == BundleScoped.class || scope == PrototypeScoped.class) {
                // nothing
            } else if (scope == Singleton.class) {
                event.setBeanAttributes(new DelegatingBeanAttributes<T>(attr) {
                    @Override
                    public Class<? extends Annotation> getScope() {
                        return SingletonScoped.class;
                    }
                });
            } else if (scope == Dependent.class) {
                if (event.getAnnotated().isAnnotationPresent(Global.class)) {
                    // @Global defaults to @PrototypeScoped
                    event.setBeanAttributes(new DelegatingBeanAttributes<T>(attr) {
                        @Override
                        public Class<? extends Annotation> getScope() {
                            return PrototypeScoped.class;
                        }
                    });
                } else {
                    // @Component defaults to @SingletonScoped
                    event.setBeanAttributes(new DelegatingBeanAttributes<T>(attr) {
                        @Override
                        public Class<? extends Annotation> getScope() {
                            return SingletonScoped.class;
                        }
                    });
                }
            } else {
                event.addDefinitionError(new IllegalArgumentException("Unsupported scope " + scope.getSimpleName() + ": " + event.getAnnotated()));
            }
        }
    }

    public <T> void processBean(@Observes ProcessBean<T> event) {
        @SuppressWarnings("unchecked")
        Bean<Object> bean = (Bean) event.getBean();
        ComponentDescriptor descriptor = null;
        if (event.getAnnotated().isAnnotationPresent(Component.class)
                || event.getAnnotated().isAnnotationPresent(Service.class)) {
            if (!event.getAnnotated().isAnnotationPresent(Component.class)
                    && !event.getAnnotated().isAnnotationPresent(Global.class)) {
                event.addDefinitionError(new IllegalArgumentException(
                        "Beans annotated with @Service " +
                                "should be annotated with @Component or @Global: " + event.getAnnotated()));
            }
            descriptor = componentRegistry.addComponent(bean);
        }
        for (InjectionPoint ip : event.getBean().getInjectionPoints()) {
            if (ip.getAnnotated().isAnnotationPresent(Service.class)
                    || ip.getAnnotated().isAnnotationPresent(Component.class)
                    || ip.getAnnotated().isAnnotationPresent(Config.class)) {
                if (ip.getAnnotated().isAnnotationPresent(Global.class)
                        || event.getAnnotated().isAnnotationPresent(Global.class)) {
                    try {
                        global.addGlobalInjectionPoint(ip);
                    } catch (IllegalArgumentException e) {
                        event.addDefinitionError(e);
                    }
                } else if (!event.getAnnotated().isAnnotationPresent(Component.class)) {
                    event.addDefinitionError(new IllegalArgumentException(
                            "Beans with @Service, @Component or @Config injection points " +
                                    "should be annotated with @Component: " + event.getAnnotated()));
                } else {
                    try {
                        descriptor.addInjectionPoint(ip);
                    } catch (IllegalArgumentException e) {
                        event.addDefinitionError(e);
                    }
                }
            }
        }
    }

    public <T, X> void processInjectionPoint(@Observes ProcessInjectionPoint<T, X> event) {
        if (event.getInjectionPoint().getAnnotated().isAnnotationPresent(Service.class)
                || event.getInjectionPoint().getAnnotated().isAnnotationPresent(Component.class)) {
            final String id = UUID.randomUUID().toString();
            event.setInjectionPoint(new DelegatingInjectionPoint(event.getInjectionPoint()) {
                public Set<Annotation> getQualifiers() {
                    Set<Annotation> annotations = new HashSet<>(delegate.getQualifiers());
                    annotations.add(new AnnotationLiteral<Service>() { });
                    annotations.add(new UniqueIdentifierLitteral(id));
                    return annotations;
                }
            });
        }
    }

    public <T> void processInjectionTarget(@Observes ProcessInjectionTarget<T> event) {
        for (InjectionPoint ip : event.getInjectionTarget().getInjectionPoints()) {
            Annotated annotated = ip.getAnnotated();
            if (annotated.isAnnotationPresent(Service.class)
                    || annotated.isAnnotationPresent(Component.class)
                    || annotated.isAnnotationPresent(Config.class)) {
//                if (annotated.isAnnotationPresent(Global.class)) {
//                    continue;
//                }
                event.setInjectionTarget(new DelegatingInjectionTarget<T>(event.getInjectionTarget()) {
                    @Override
                    public void inject(T instance, CreationalContext<T> ctx) {
                        super.inject(instance, ctx);
                        for (InjectionPoint injectionPoint : getInjectionPoints()) {
                            ComponentDescriptor descriptor = componentRegistry.getDescriptor(injectionPoint.getBean());
                            if (descriptor != null) {
                                descriptor.inject(instance, injectionPoint);
                            }
                        }
                    }
                });
                return;
            }
        }
    }


    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        componentRegistry.preStart(event, global);

        BeanManager beanManager = componentRegistry.getBeanManager();
        SingletonScopeContext serviceContext = new SingletonScopeContext(beanManager);
        event.addContext(serviceContext);
        BundleScopeContext bundleScopeContext = new BundleScopeContext(beanManager);
        event.addContext(bundleScopeContext);
        PrototypeScopeContext prototypeScopeContext = new PrototypeScopeContext(beanManager);
        event.addContext(prototypeScopeContext);
    }

    public void applicationScopeInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
        componentRegistry.start();
        Registry.getInstance().register(componentRegistry);
    }

    public void applicationScopeDestroyed(@Observes @Destroyed(ApplicationScoped.class) Object destroy) {
        Registry.getInstance().unregister(componentRegistry);
        componentRegistry.stop();
    }

    @Target({METHOD, FIELD, PARAMETER, TYPE})
    @Retention(RUNTIME)
    @Qualifier
    public @interface UniqueIdentifier {
        String id();
    }

    static class UniqueIdentifierLitteral extends AnnotationLiteral<UniqueIdentifier> implements UniqueIdentifier {
        private final String id;
        UniqueIdentifierLitteral(String id) {
            this.id = id;
        }
        @Override
        public String id() {
            return id;
        }
    }

    public Set<String> getObservedFilters() {
        return observedFilters;
    }

    public Set<Annotation> getObservedQualifiers() {
        return observedQualifiers;
    }

    public <T, X> void processObserverMethod(@Observes ProcessObserverMethod<T, X> event) {
        Set<Annotation> qualifiers = event.getObserverMethod().getObservedQualifiers();
        if (qualifiers.contains(new ServiceAddedLiteral())
                || qualifiers.contains(new ServiceRemovedLiteral())) {
            List<String> filters = Filters.getSubFilters(qualifiers);
            Type observed = event.getObserverMethod().getObservedType();
            Class service = Types.getRawType(observed);
            if (service == ServiceCdiEvent.class) {
                service = Types.getRawType(((ParameterizedType) observed).getActualTypeArguments()[0]);
            }
            if (service != Object.class) {
                String subfilter = "(" + Constants.OBJECTCLASS + "=" + service.getName() + ")";
                filters.add(0, subfilter);
            }
            String filter = Filters.and(filters);
            observedFilters.add(filter);
            observedQualifiers.addAll(qualifiers);
        }
    }

}
