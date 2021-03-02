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
package org.ops4j.pax.cdi.openwebbeans.impl;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.ops4j.pax.cdi.spi.AbstractCdiContainer;
import org.ops4j.pax.cdi.spi.CdiClassLoaderBuilderCustomizer;
import org.ops4j.pax.cdi.spi.util.Exceptions;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code CdiContainer} implementation wrapping an Apache OpenWebBeans container, represented by a
 * {@link WebBeansContext}.
 *
 * @author Harald Wellmann
 *
 */
public class OpenWebBeansCdiContainer extends AbstractCdiContainer {

    private static Logger log = LoggerFactory.getLogger(OpenWebBeansCdiContainer.class);

    /**
     * OpenWebBeans container lifecycle.
     */
    private ContainerLifecycle lifecycle;

    /**
     * Helper for accessing Instance and Event of CDI container.
     */
    private InstanceManager instanceManager;

    private WebBeansContext context;

    /**
     * Construct a CDI container for the given extended bundle.
     *
     * @param ownBundle
     *            bundle containing this class
     * @param extendedBundle
     *            bundle to be extended with CDI container
     * @param extensionBundles
     *            CDI extension bundles to be loaded by OpenWebBeans
     */
    public OpenWebBeansCdiContainer(Bundle ownBundle,
        Bundle extendedBundle, Collection<Bundle> extensionBundles) {
        super(extendedBundle, extensionBundles, Collections.singletonList(ownBundle));
        log.debug("creating OpenWebBeans CDI container for bundle {}", extendedBundle);
    }

    /**
     * Creates and starts a WebBeansContext for the given bundle using an appropriate class loader
     * as TCCL.
     *
     * @param bundle
     * @return
     */
    private WebBeansContext createWebBeansContext(final Object environment) {
        try {
            return doWithClassLoader(new Callable<WebBeansContext>() {

                @Override
                public WebBeansContext call() throws Exception {
                    WebBeansContext webBeansContext = WebBeansContext.currentInstance();
                    lifecycle = webBeansContext.getService(ContainerLifecycle.class);
                    lifecycle.startApplication(environment);
                    return webBeansContext;
                }
            });
        }
        // CHECKSTYLE:SKIP
        catch (Exception exc) {
            throw Exceptions.unchecked(exc);
        }
    }

    @Override
    protected void doStart(Object environment) {
        context = createWebBeansContext(environment);
        if (log.isDebugEnabled()) {
            for (Bean<?> bean : context.getBeanManagerImpl().getBeans()) {
                log.debug("  {}", bean);
            }
        }
    }

    @Override
    protected void doStop() {
        try {
            doWithClassLoader(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    if (lifecycle != null) {
                        lifecycle.stopApplication(getContextClassLoader());
                    }
                    return null;
                }
            });
        }
        // CHECKSTYLE:SKIP
        catch (Exception exc) {
            throw Exceptions.unchecked(exc);
        }
    }

    @Override
    public Event<Object> getEvent() {
        return getInstanceManager().getEvent();
    }

    @Override
    public BeanManager getBeanManager() {
        return lifecycle.getBeanManager();
    }

    @Override
    public Instance<Object> getInstance() {
        return getInstanceManager().getInstance();
    }

    private InstanceManager getInstanceManager() {
        if (instanceManager == null) {
            BeanManager beanManager = getBeanManager();
            instanceManager = new InstanceManager();
            AnnotatedType<InstanceManager> annotatedType = beanManager
                .createAnnotatedType(InstanceManager.class);
            InjectionTarget<InstanceManager> target = beanManager
                .createInjectionTarget(annotatedType);
            CreationalContext<InstanceManager> cc = beanManager.createCreationalContext(null);
            target.inject(instanceManager, cc);
        }
        return instanceManager;
    }

    @Override
    public <T> T unwrap(Class<T> wrappedClass) {
        if (wrappedClass.isAssignableFrom(WebBeansContext.class)) {
            return wrappedClass.cast(context);
        }
        if (wrappedClass.isAssignableFrom(ContainerLifecycle.class)) {
            return wrappedClass.cast(lifecycle);
        }
        if (wrappedClass.isAssignableFrom(CdiClassLoaderBuilderCustomizer.class)) {
            return wrappedClass.cast(this);
        }
        return null;
    }

    @Override
    public void startContext(Class<? extends Annotation> scope) {
        ContextsService contextsService = context.getService(ContextsService.class);
        contextsService.startContext(scope, null);

    }

    @Override
    public void stopContext(Class<? extends Annotation> scope) {
        ContextsService contextsService = context.getService(ContextsService.class);
        contextsService.endContext(scope, null);
    }
}
