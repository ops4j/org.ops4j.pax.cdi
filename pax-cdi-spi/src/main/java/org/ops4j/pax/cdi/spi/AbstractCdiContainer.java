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
package org.ops4j.pax.cdi.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.xbean.osgi.bundle.util.BundleClassLoader;
import org.apache.xbean.osgi.bundle.util.DelegatingBundle;
import org.ops4j.pax.cdi.spi.util.Exceptions;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for {@link CdiContainer} implementations.
 *
 * @author Harald Wellmann
 */
public abstract class AbstractCdiContainer implements CdiContainer, CdiClassLoaderBuilderCustomizer {

    private static Logger log = LoggerFactory.getLogger(AbstractCdiContainer.class);

    private Bundle bundle;
    private ServiceRegistration<CdiContainer> cdiContainerReg;
    private ServiceRegistration<BeanManager> beanManagerReg;
    private boolean started;

    /**
     * All CDI extension bundles discovered by the Pax CDI extender before creating the
     * CdiContainerFactory.
     */
    private Collection<Bundle> extensionBundles;

    /**
     * Any additional bundles to be included in the composite context class loader. This must
     * include at least the adapter bundle containing the concrete implementation of this abstract
     * class.
     */
    private Collection<Bundle> additionalBundles;

    /**
     * A composite class loader used as thread context class loader for the CDI provider. This class
     * loader delegates to the bundle class loaders of the extended bundle, its extension bundle and
     * any required additional bundles.
     */
    private ClassLoader contextClassLoader;

    /**
     * We may configure an external builder that'll create {@link #contextClassLoader} instead of
     * building our own.
     */
    private CdiClassLoaderBuilder builder;

    protected AbstractCdiContainer(Bundle bundle,
        Collection<Bundle> extensionBundles, Collection<Bundle> additionalBundles) {
        this.bundle = bundle;
        this.extensionBundles = extensionBundles;
        this.additionalBundles = additionalBundles;
    }

    @Override
    public synchronized void start(Object environment) {
        if (!started) {
            log.info("Starting CDI container for bundle {}", getBundle());
            contextClassLoader = buildContextClassLoader(environment);
            BeanBundles.addBundle(getContextClassLoader(), getBundle());
            doStart(environment);
            finishStartup();
            started = true;
        }
    }

    @Override
    public synchronized void stop() {
        if (started) {
            log.info("Stopping CDI container for bundle {}", getBundle());
            doStop();
            BeanBundles.removeBundle(getContextClassLoader(), getBundle());
            unregister(cdiContainerReg);
            unregister(beanManagerReg);
            started = false;
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    private <S> void unregister(ServiceRegistration<S> registration) {
        if (registration != null) {
            try {
                registration.unregister();
            }
            catch (IllegalStateException exc) {
                log.trace("service already unregistered", exc);
            }
        }
    }

    protected abstract void doStart(Object environment);

    protected abstract void doStop();

    /**
     * Builds the composite class loader for the given bundle, also including the bundle containing
     * this class and all extension bundles.
     */
    protected ClassLoader buildContextClassLoader(Object environment) {
        List<Bundle> delegateBundles = new ArrayList<>();
        delegateBundles.add(bundle);
        delegateBundles.addAll(additionalBundles);
        delegateBundles.addAll(extensionBundles);

        if (builder != null) {
            // in pax-web we may already have necessary classloader - possibly without
            // "additionalBundles"
            ClassLoader cl = builder.buildContextClassLoader(environment, bundle, extensionBundles, additionalBundles);
            if (cl != null) {
                log.info("Using provided contextClassLoader: {}", cl);
                return cl;
            }
        }

        // fallback to creating xbean classloader
        DelegatingBundle delegatingBundle = new DelegatingBundle(delegateBundles);
        BundleClassLoader classLoader = new BundleClassLoader(delegatingBundle);
        log.info("Using default contextClassLoader: {}", classLoader);
        return classLoader;
    }

    @Override
    public ClassLoader getContextClassLoader() {
        return contextClassLoader;
    }

    public <V> V doWithClassLoader(final Callable<V> callable) throws Exception {
        Thread currentThread = Thread.currentThread();
        ClassLoader prevTccl = currentThread.getContextClassLoader();
        try {
            currentThread.setContextClassLoader( getContextClassLoader() );
            return callable.call();
        }
        finally {
            currentThread.setContextClassLoader(prevTccl);
        }
    }

    private void finishStartup() {
        try {
            cdiContainerReg = doWithClassLoader(
                new Callable<ServiceRegistration<CdiContainer>>() {

                    @Override
                    public ServiceRegistration<CdiContainer> call() throws Exception {
                        return registerCdiContainer();
                    }
                });
        }
        // CHECKSTYLE:SKIP
        catch (Exception exc) {
            log.error("", exc);
            throw Exceptions.unchecked(exc);
        }
    }

    private ServiceRegistration<CdiContainer> registerCdiContainer() {
        BundleContext bc = bundle.getBundleContext();

        // fire ContainerInitialized event
        BeanManager beanManager = getBeanManager();
        beanManager.fireEvent(new ContainerInitialized());

        // register CdiContainer service
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("bundleId", bundle.getBundleId());
        props.put("symbolicName", bundle.getSymbolicName());

        ServiceRegistration<CdiContainer> reg = bc.registerService(
            CdiContainer.class, AbstractCdiContainer.this, props);

        beanManagerReg = bc.registerService(
            BeanManager.class, AbstractCdiContainer.this.getBeanManager(), props);

        return reg;
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public void setCdiClassLoaderBuilder(CdiClassLoaderBuilder builder) {
        this.builder = builder;
    }

}
