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

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.ops4j.pax.cdi.spi.AbstractCdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerType;
import org.ops4j.pax.cdi.weld.impl.bda.BundleDeployment;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link CdiContainer} implementation wrapping a JBoss Weld container, represented by a
 * {@link WeldBootstrap}.
 * 
 * @author Harald Wellmann
 * 
 */
public class WeldCdiContainer extends AbstractCdiContainer {

    private Logger logger = LoggerFactory.getLogger(WeldCdiContainer.class);

    /** Bundle owning this class. */
    private Bundle ownBundle;

    private Collection<URL> descriptors;

    /**
     * All CDI extension bundles discovered by the Pax CDI extender before creating the
     * CdiContainerFactory.
     */
    private Collection<Bundle> extensionBundles;

    /**
     * A composite class loader used as thread context class loader for Weld. This class loader
     * delegates to the bundle class loaders of our own bundle, the extended bundle and all
     * extension bundles.
     */
    private BundleClassLoader contextClassLoader;

    /**
     * Helper for accessing Instance and Event of CDI container.
     */
    private InstanceManager instanceManager;

    private WeldBootstrap bootstrap;

    private BeanManagerImpl manager;

    /**
     * Construct a CDI container for the given extended bundle.
     * 
     * @param ownBundle
     *            bundle containing this class
     * @param bundle
     *            bundle to be extended with CDI container
     * @param extensionBundles
     *            CDI extension bundles to be loaded by OpenWebBeans
     */
    public WeldCdiContainer(CdiContainerType containerType, Bundle ownBundle,
        Bundle bundle, Collection<URL> descriptors, Collection<Bundle> extensionBundles) {
        super(containerType, bundle);
        logger.debug("creating Weld CDI container for bundle {}", bundle);
        this.ownBundle = ownBundle;
        this.descriptors = descriptors;
        this.extensionBundles = extensionBundles;
    }

    @Override
    protected void doStart(Object environment) {
        // Creates and starts a WebBeansContext for the given bundle using an
        // appropriate class loader as TCCL.
        buildContextClassLoader();
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(contextClassLoader);
        try {
            bootstrap = new WeldBootstrap();
            BundleDeployment deployment = new BundleDeployment(getBundle(), descriptors, bootstrap);
            BeanDeploymentArchive beanDeploymentArchive = deployment.getBeanDeploymentArchive();

            String contextId = getBundle().getSymbolicName() + ":" + getBundle().getBundleId();
            bootstrap.startContainer(contextId, OsgiEnvironment.getInstance(), deployment);
            bootstrap.startInitialization();
            bootstrap.deployBeans();
            bootstrap.validateBeans();
            bootstrap.endInitialization();
            manager = bootstrap.getManager(beanDeploymentArchive);
        }
        finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    @Override
    public void doStop() {
        bootstrap.shutdown();
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

    /**
     * Builds the composite class loader for the given bundle, also including the bundle containing
     * this class and all extension bundles.
     */
    private void buildContextClassLoader() {
        Bundle bundle = getBundle();
        Set<Bundle> delegateBundles = new HashSet<Bundle>();
        delegateBundles.add(bundle);
        delegateBundles.add(ownBundle);
        delegateBundles.add(FrameworkUtil.getBundle(Bootstrap.class));

        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        List<BundleWire> wires = wiring.getRequiredWires(BundleRevision.BUNDLE_NAMESPACE);
        for (BundleWire wire : wires) {
            delegateBundles.add(wire.getProviderWiring().getBundle());
        }
        delegateBundles.addAll(extensionBundles);
        DelegatingBundle delegatingBundle = new DelegatingBundle(delegateBundles);
        contextClassLoader = new BundleClassLoader(delegatingBundle);
    }

    private InstanceManager getInstanceManager() {
        if (instanceManager == null) {
            BeanManager beanManager = getBeanManager();
            instanceManager = new InstanceManager();
            AnnotatedType<InstanceManager> annotatedType = beanManager.createAnnotatedType(InstanceManager.class);
            InjectionTarget<InstanceManager> target = beanManager.createInjectionTarget(annotatedType);
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

    @Override
    public void startContext(Class<? extends Annotation> scope) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void stopContext(Class<? extends Annotation> scope) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
