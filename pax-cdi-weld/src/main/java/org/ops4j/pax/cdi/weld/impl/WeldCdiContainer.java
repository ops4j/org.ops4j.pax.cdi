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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.manager.BeanManagerImpl;
import org.ops4j.pax.cdi.spi.AbstractCdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerType;
import org.ops4j.pax.cdi.spi.DestroyedLiteral;
import org.ops4j.pax.cdi.spi.InitializedLiteral;
import org.ops4j.pax.cdi.spi.util.Exceptions;
import org.ops4j.pax.cdi.weld.impl.bda.BundleDeployment;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
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

    private Logger log = LoggerFactory.getLogger(WeldCdiContainer.class);

    /**
     * Helper for accessing Instance and Event of CDI container.
     */
    private InstanceManager instanceManager;

    private WeldBootstrap bootstrap;

    private BeanManagerImpl manager;

    private Object environment;

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
    public WeldCdiContainer(CdiContainerType containerType, Bundle ownBundle, Bundle bundle,
        Collection<Bundle> extensionBundles) {
        super(containerType, bundle, extensionBundles, Arrays.asList(ownBundle,
            FrameworkUtil.getBundle(Bootstrap.class)));
        log.debug("creating Weld CDI container for bundle {}", bundle);
    }

    @Override
    protected void doStart(Object environment) {
        this.environment = environment;
        try {
            doWithClassLoader(getContextClassLoader(), new Callable<BeanManager>() {

                @Override
                public BeanManager call() throws Exception {
                    return createBeanManager();
                }
            });
        }
        // CHECKSTYLE:SKIP
        catch (Exception exc) {
            throw Exceptions.unchecked(exc);
        }
    }

    private BeanManager createBeanManager() {
        bootstrap = new WeldBootstrap();
        BundleDeployment deployment = new BundleDeployment(getBundle(), bootstrap, getContextClassLoader());
        BeanDeploymentArchive beanDeploymentArchive = deployment
            .getBeanDeploymentArchive();

        String contextId = getBundle().getSymbolicName() + ":"
            + getBundle().getBundleId();
        bootstrap.startContainer(contextId, OsgiEnvironment.getInstance(), deployment);
        bootstrap.startInitialization();
        bootstrap.deployBeans();
        bootstrap.validateBeans();
        bootstrap.endInitialization();
        manager = bootstrap.getManager(beanDeploymentArchive);
        manager.fireEvent(environment, InitializedLiteral.APPLICATION);
        return manager;
    }

    @Override
    public void doStop() {
        try {
            doWithClassLoader(getContextClassLoader(), new Callable<Object>() {

                @Override
                public Object call() throws Exception {
                    manager.fireEvent(environment, DestroyedLiteral.APPLICATION);
                    bootstrap.shutdown();
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
