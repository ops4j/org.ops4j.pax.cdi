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
package org.ops4j.pax.cdi.weld.impl;

import static org.ops4j.pax.swissbox.core.ContextClassLoaderUtils.doWithClassLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.apache.xbean.osgi.bundle.util.BundleClassLoader;
import org.apache.xbean.osgi.bundle.util.DelegatingBundle;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.manager.BeanManagerImpl;
import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.weld.impl.bda.BundleDeployment;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link CdiContainer} implementation wrapping a JBoss Weld container, represented by a
 * {@link WeldBootstrap}.
 * 
 * @author Harald Wellmann
 * 
 */
public class WeldCdiContainer implements CdiContainer {

    private Logger logger = LoggerFactory.getLogger(WeldCdiContainer.class);

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
     * A composite class loader used as thread context class loader for OpenWebBeans. This class
     * loader delegates to the bundle class loaders of our own bundle, the extended bundle and all
     * extension bundles.
     */
    private BundleClassLoader contextClassLoader;

    /**
     * Helper for accessing Instance and Event of CDI container.
     */
    private InstanceManager instanceManager;

    private boolean started;

    private WeldBootstrap bootstrap;

    private BeanManagerImpl manager;

    private boolean hasShutdownBeenCalled = false;

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
    public WeldCdiContainer(Bundle ownBundle, Bundle extendedBundle,
        Collection<Bundle> extensionBundles) {
        logger.debug("creating Weld CDI container for bundle {}", extendedBundle);
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
    private Void createWeldContainer(Bundle bundle) {
        buildContextClassLoader(bundle);
        try {
            return doWithClassLoader(contextClassLoader, new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    try {
                        initialize();
                        return null;
                    }
                    catch (Throwable exc) {
                        logger.error("", exc);
                        throw new Ops4jException(exc);
                    }
                }
            });
        }
        catch (Exception exc) {
            logger.error("", exc);
            throw new Ops4jException(exc);
        }
    }

    public boolean initialize() {
        started = false;
        bootstrap = new WeldBootstrap();
        BundleDeployment deployment = new BundleDeployment(extendedBundle, bootstrap);
        BeanDeploymentArchive beanDeploymentArchive = deployment.getBeanDeploymentArchive();

        String contextId = extendedBundle.getSymbolicName() + ":" + extendedBundle.getBundleId();
        bootstrap.startContainer(contextId, OsgiEnvironment.getInstance(), deployment);
        bootstrap.startInitialization();
        bootstrap.deployBeans();
        bootstrap.validateBeans();
        bootstrap.endInitialization();
        manager = bootstrap.getManager(beanDeploymentArchive);
        started = true;
        return started;
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
        BundleReference bundleRef = BundleReference.class.cast(Bootstrap.class.getClassLoader());
        delegateBundles.add(bundleRef.getBundle());
        delegateBundles.addAll(extensionBundles);
        DelegatingBundle delegatingBundle = new DelegatingBundle(delegateBundles);
        contextClassLoader = new BundleClassLoader(delegatingBundle);
    }

    @Override
    public Bundle getBundle() {
        return extendedBundle;
    }

    @Override
    public void stop() {
        logger.debug("Weld CDI container is shutting down for bundle {}", extendedBundle);
        if (started) {
            synchronized (this) {
                if (!hasShutdownBeenCalled) {
                    logger.info("Stopping Weld instance for bundle {}", extendedBundle);
                    hasShutdownBeenCalled = true;
                    try {
                        bootstrap.shutdown();
                    }
                    catch (Throwable t) {
                        logger.error(extendedBundle.getSymbolicName() + ": error on CDI container shutdown", t);
                    }
                    started = false;
                }
            }
        }
    }

    @Override
    public void start() {
        createWeldContainer(extendedBundle);
    }

    @Override
    public Event<Object> getEvent() {
        return getInstanceManager().getEvent();
    }

    @Override
    public BeanManager getBeanManager() {
        return manager;
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
		if (wrappedClass.isAssignableFrom(WeldBootstrap.class)) {
			return wrappedClass.cast(bootstrap);
		}
		if (wrappedClass.isAssignableFrom(BeanManagerImpl.class)) {
			return wrappedClass.cast(manager);
		}
		return null;
	}
}
