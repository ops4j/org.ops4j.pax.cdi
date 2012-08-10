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

import static org.ops4j.pax.swissbox.core.ContextClassLoaderUtils.doWithClassLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Singleton;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.apache.xbean.osgi.bundle.util.BundleClassLoader;
import org.apache.xbean.osgi.bundle.util.DelegatingBundle;
import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.swissbox.lifecycle.Lifecycle;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link CdiContainer} implementation wrapping an Apache OpenWebBeans container, represented by a
 * {@link WebBeansContext}.
 * 
 * @author Harald Wellmann
 * 
 */
public class OpenWebBeansCdiContainer implements CdiContainer {

    private Logger logger = LoggerFactory.getLogger(OpenWebBeansCdiContainer.class);

    /** Bundle owning this class. */
    private Bundle ownBundle;

    /** The bundle extended by this CDI container. */
    private Bundle extendedBundle;

    /**
     * All CDI extension bundles discovered by the Pax CDI extender before creating the
     * CdiContainerFactory.
     */
    private Collection<Bundle> extensionBundles;

    /**
     * OpenWebBeans container lifecycle.
     */
    private ContainerLifecycle lifecycle;

    /**
     * A composite class loader used as thread context class loader for OpenWebBeans. This class
     * loader delegates to the bundle class loaders of our own bundle, the extended bundle and all
     * extension bundles.
     */
    private BundleClassLoader contextClassLoader;

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
    public OpenWebBeansCdiContainer(Bundle ownBundle, Bundle extendedBundle,
        Collection<Bundle> extensionBundles) {
        logger.debug("creating OpenWebBeans CDI container for bundle {}", extendedBundle);
        this.ownBundle = ownBundle;
        this.extendedBundle = extendedBundle;
        this.extensionBundles = extensionBundles;
    }

    /**
     * Creates and starts a WebBeansContext for the given bundle using an appropriate class loader
     * as TCCL.
     * 
     * @param bundle
     * @return
     */
    private WebBeansContext createWebBeansContext(Bundle bundle) {
        buildContextClassLoader(bundle);
        try {
            return doWithClassLoader(contextClassLoader, new Callable<WebBeansContext>() {

                @Override
                public WebBeansContext call() throws Exception {
                    WebBeansContext webBeansContext = WebBeansContext.currentInstance();
                    lifecycle = webBeansContext.getService(ContainerLifecycle.class);
                    lifecycle.startApplication(contextClassLoader);
                    startContexts(webBeansContext);
                    return webBeansContext;
                }
            });
        }
        catch (Exception exc) {
            throw new Ops4jException(exc);
        }
    }

    /**
     * Builds the composite class loader for the given bundle, also including the bundle containing
     * this class and all extension bundles.
     * 
     * @param bundle
     */
    private void buildContextClassLoader(Bundle bundle) {
        List<Bundle> delegateBundles = new ArrayList<Bundle>();
        delegateBundles.add(bundle);
        delegateBundles.add(ownBundle);
        delegateBundles.addAll(extensionBundles);
        DelegatingBundle delegatingBundle = new DelegatingBundle(delegateBundles);
        contextClassLoader = new BundleClassLoader(delegatingBundle);
    }

    /**
     * Starts all CDI contexts.
     * 
     * @param webBeansContext
     */
    private void startContexts(WebBeansContext webBeansContext) {
        ContextFactory contextFactory = webBeansContext.getContextFactory();

        contextFactory.initSingletonContext(null);
        contextFactory.initApplicationContext(null);
        contextFactory.initSessionContext(null);
        contextFactory.initConversationContext(null);
        contextFactory.initRequestContext(null);
    }

    /**
     * Stops all CDI contexts.
     */
    private void stopContexts() {
        ContextsService contextService = lifecycle.getContextService();
        contextService.endContext(RequestScoped.class, null);
        contextService.endContext(ConversationScoped.class, null);
        contextService.endContext(SessionScoped.class, null);
        contextService.endContext(ApplicationScoped.class, null);
        contextService.endContext(Singleton.class, null);
    }

    @Override
    public Bundle getBundle() {
        return extendedBundle;
    }

    @Override
    public void stop() {
        logger.debug("OpenWebBeans CDI container is shutting down for bundle {}", extendedBundle);
        stopContexts();
        lifecycle.stopApplication(null);
    }

    @Override
    public void start() {
        context = createWebBeansContext(extendedBundle);
        for (Bean<?> bean : context.getBeanManagerImpl().getBeans()) {
            logger.debug("  {}", bean);
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
    public ClassLoader getContextClassLoader() {
        return contextClassLoader;
    }
    
	@Override
	public <T> T unwrap(Class<T> wrappedClass) {
		if (wrappedClass.isAssignableFrom(WebBeansContext.class)) {
			return wrappedClass.cast(context);
		}
		if (wrappedClass.isAssignableFrom(Lifecycle.class)) {
			return wrappedClass.cast(lifecycle);
		}
		return null;
	}
    
}
