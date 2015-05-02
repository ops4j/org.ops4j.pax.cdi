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
package org.ops4j.pax.cdi.extension.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.ProcessInjectionTarget;

import org.ops4j.pax.cdi.api.OsgiService;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.ops4j.pax.cdi.api.SingletonScoped;
import org.ops4j.pax.cdi.extension.impl.client.OsgiInjectionTarget;
import org.ops4j.pax.cdi.extension.impl.client.OsgiInjectionTargetWrapper;
import org.ops4j.pax.cdi.extension.impl.client.OsgiServiceBean;
import org.ops4j.pax.cdi.extension.impl.component.ComponentLifecycleManager;
import org.ops4j.pax.cdi.extension.impl.component.ComponentRegistry;
import org.ops4j.pax.cdi.extension.impl.context.BundleScopeContext;
import org.ops4j.pax.cdi.extension.impl.context.PrototypeScopeContext;
import org.ops4j.pax.cdi.extension.impl.context.SingletonScopeContext;
import org.ops4j.pax.cdi.extension.impl.util.InjectionPointOsgiUtils;
import org.ops4j.pax.cdi.extension.impl.util.WrappedBeanAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of Pax CDI OSGi extension which injects OSGi services into CDI injection points and
 * publishes CDI beans as OSGi services.
 *
 * @author Harald Wellmann
 *
 */
@ApplicationScoped
public class OsgiExtension implements Extension {

    private static Logger log = LoggerFactory.getLogger(OsgiExtension.class);

    /** Maps service types to injection points. */
    private Map<Type, Set<InjectionPoint>> typeToIpMap = new HashMap<>();

    /**
     * Registry for service components and their dependencies.
     */
    private ComponentRegistry componentRegistry = new ComponentRegistry();

    private SingletonScopeContext serviceContext;

    public OsgiExtension() {
        log.debug("constructing OsgiExtension");
    }

    /**
     * BeforeBeanDiscovery observer which creates some additional beans and the Service Scope for
     * OSGi components.
     *
     * @param event
     * @param manager
     */
    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager manager) {
        log.debug("beforeBeanDiscovery");
        event.addAnnotatedType(manager.createAnnotatedType(BeanBundleImpl.class));
        event.addAnnotatedType(manager.createAnnotatedType(BundleEventBridge.class));
        event.addAnnotatedType(manager.createAnnotatedType(ServiceEventBridge.class));
        event.addAnnotatedType(manager.createAnnotatedType(BundleContextProducer.class));
        event.addAnnotatedType(manager.createAnnotatedType(ComponentLifecycleManager.class));
        event.addAnnotatedType(manager.createAnnotatedType(OsgiInjectionTargetWrapper.class));
        event.addScope(SingletonScoped.class, false, false);
    }

    /**
     * ProcessBeanAttributes observer for {@code OsgiServiceProvider} beans. If the bean does not
     * have an explicit OSGi scope, the scope is set to {@code SingletonScoped}.
     *
     * @param event
     *            bean attributes event
     */
    public <T> void processBeanAttributes(@Observes ProcessBeanAttributes<T> event) {
        if (event.getAnnotated().getAnnotation(OsgiServiceProvider.class) == null) {
            return;
        }

        final BeanAttributes<T> attributes = event.getBeanAttributes();
        if (!attributes.getScope().equals(Dependent.class)) {
            return;
        }

        BeanAttributes<T> wrappedAttributes = new WrappedBeanAttributes<T>() {

            @Override
            protected BeanAttributes<T> attributes() {
                return attributes;
            }

            @Override
            public Class<? extends Annotation> getScope() {
                return SingletonScoped.class;
            }
        };

        event.setBeanAttributes(wrappedAttributes);
    }

    /**
     * ProcessInjectionTarget observer which scans for OSGi service injection points.
     *
     * @param event
     */
    public <T> void processInjectionTarget(@Observes ProcessInjectionTarget<T> event) {
        log.debug("processInjectionTarget {}", event.getAnnotatedType().getBaseType());
        boolean overrideRequired = false;
        for (InjectionPoint ip : event.getInjectionTarget().getInjectionPoints()) {
            boolean instanceIp = processInjectionPoint(ip);
            overrideRequired |= instanceIp;
        }
        if (overrideRequired) {
            event.setInjectionTarget(new OsgiInjectionTarget<T>(event.getInjectionTarget()));
        }
    }

    /**
     * Returns true if the injection point has type Instance<T> for an OsgiService, so we need to
     * override the injection target.
     *
     * @param ip
     * @return
     */
    private boolean processInjectionPoint(InjectionPoint ip) {
        OsgiService qualifier = ip.getAnnotated().getAnnotation(OsgiService.class);
        if (qualifier != null) {
            log.debug("service injection point {} with qualifier {}", ip, qualifier);
            storeServiceInjectionPoint(ip);
        }
        Type instanceType = InjectionPointOsgiUtils.getInstanceType(ip);
        return instanceType != null;
    }

    /**
     * Stores the given injection point in the {@code typeToIpMap}.
     *
     * @param injectionPoint
     */
    private void storeServiceInjectionPoint(InjectionPoint injectionPoint) {
        Type key = injectionPoint.getType();
        if (!typeToIpMap.containsKey(key)) {
            typeToIpMap.put(key, new CopyOnWriteArraySet<InjectionPoint>());
        }
        typeToIpMap.get(key).add(injectionPoint);
    }

    /**
     * ProcessBean observer which registers OSGi components and their service dependencies in the
     * {@link ComponentRegistry}.
     *
     * @param event
     */
    public <T> void processBean(@Observes ProcessBean<T> event) {
        Bean<T> bean = event.getBean();
        log.debug("processBean {}", bean);

        OsgiServiceProvider qualifier = event.getAnnotated().getAnnotation(
            OsgiServiceProvider.class);
        if (qualifier != null) {
            componentRegistry.addComponent(bean);
            for (InjectionPoint ip : bean.getInjectionPoints()) {
                OsgiService annotation = ip.getAnnotated().getAnnotation(OsgiService.class);
                if (annotation != null) {
                    componentRegistry.addDependency(bean, ip);
                }
            }
        }
    }

    /**
     * AfterBeanDiscovery observer which registers {@code OsgiServiceBean}s for all types required
     * by OSGi service injection points.
     *
     * @param event
     */
    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        log.debug("afterBeanDiscovery");
        serviceContext = new SingletonScopeContext(beanManager);
        event.addContext(serviceContext);
        BundleScopeContext bundleScopeContext = new BundleScopeContext(beanManager);
        event.addContext(bundleScopeContext);
        PrototypeScopeContext prototypeScopeContext = new PrototypeScopeContext(beanManager);
        event.addContext(prototypeScopeContext);
        for (Entry<Type, Set<InjectionPoint>> entry : typeToIpMap.entrySet()) {
            Type type = entry.getKey();
            if (isInstance(type)) {
                // handled by OsgiInjectionTarget
            }
            else if (type instanceof Class) {
                addBean(event, type, entry.getValue());
            }
            else {
                InjectionPoint ip = typeToIpMap.get(type).iterator().next();
                String msg = "The type of an @OSGi service injection point must not be parameterized. Injection point = "
                    + ip;
                event.addDefinitionError(new UnsupportedOperationException(msg));
                continue;
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void addBean(AfterBeanDiscovery event, Type type, Set<InjectionPoint> injectionPoints) {
        List<OsgiService> registeredBeans = new ArrayList<>();
        for (InjectionPoint ip : injectionPoints) {
            OsgiService qualifier = ip.getAnnotated().getAnnotation(OsgiService.class);
            if (!registeredBeans.contains(qualifier)) {
                log.debug("adding an OSGi service bean {} for {}", type, ip);

                event.addBean(new OsgiServiceBean(ip));
                registeredBeans.add(qualifier);

                if (!qualifier.dynamic() && !componentRegistry.isComponent(ip.getBean())) {
                    componentRegistry.addNonComponentDependency(ip);
                }
            }
        }
    }

    private boolean isInstance(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            return Instance.class.isAssignableFrom(rawType);
        }
        return false;
    }

    ComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }

    SingletonScopeContext getServiceContext() {
        return serviceContext;
    }
}
