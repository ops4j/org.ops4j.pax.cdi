/*
 * Copyright 2015 Harald Wellmann.
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
package org.ops4j.pax.cdi.extender.impl;

import static org.ops4j.pax.cdi.spi.BeanBundles.findExtensions;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;

import org.ops4j.pax.cdi.spi.CdiClassLoaderBuilder;
import org.ops4j.pax.cdi.spi.CdiClassLoaderBuilderCustomizer;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delegates to a real CDI container which gets recreated whenever {@code start()} is called. This
 * is used to correctly implement the lifecycle of web beans bundles for which the CDI container
 * must be destroyed and recreated when the web extender is restarted, while the web bean bundle
 * itself remains active.
 *
 * @author Harald Wellmann
 *
 */
public class DelegatingCdiContainer implements CdiContainer, CdiClassLoaderBuilderCustomizer {

    private static Logger log = LoggerFactory.getLogger(DelegatingCdiContainer.class);

    private CdiContainerFactory factory;
    private Bundle bundle;
    private CdiContainer delegate;

    /**
     * We may configure an external builder that'll create context ClassLoader instead of
     * building our own.
     */
    private CdiClassLoaderBuilder builder;

    public DelegatingCdiContainer(CdiContainerFactory factory, Bundle bundle) {
        this.factory = factory;
        this.bundle = bundle;
    }

    @Override
    public synchronized void start(Object environment) {
        Set<Bundle> extensions = new HashSet<>();
        findExtensions(bundle, extensions);

        log.info("creating CDI container for bean bundle {} with extension bundles {}", bundle, extensions);
        delegate = factory.createContainer(bundle, extensions);

        if (builder != null) {
            CdiClassLoaderBuilderCustomizer customizer = delegate.unwrap(CdiClassLoaderBuilderCustomizer.class);
            if (customizer != null) {
                customizer.setCdiClassLoaderBuilder(builder);
            }
        }

        delegate.start(environment);
    }

    @Override
    public synchronized void stop() {
        if (delegate != null) {
            delegate.stop();
            delegate = null;
        }
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public synchronized BeanManager getBeanManager() {
        if (delegate != null) {
            return delegate.getBeanManager();
        }
        return null;
    }

    @Override
    public <T> Event<T> getEvent() {
        if (delegate != null) {
            return delegate.getEvent();
        }
        return null;
    }

    @Override
    public <T> Instance<T> getInstance() {
        if (delegate != null) {
            return delegate.getInstance();
        }
        return null;
    }

    @Override
    public ClassLoader getContextClassLoader() {
        return delegate.getContextClassLoader();
    }

    @Override
    public <V> V doWithClassLoader(Callable<V> callable) throws Exception {
        return delegate.doWithClassLoader(callable);
    }

    @Override
    public <T> T unwrap(Class<T> wrappedClass) {
        if (wrappedClass.isAssignableFrom(CdiClassLoaderBuilderCustomizer.class)) {
            return wrappedClass.cast(this);
        }
        return delegate.unwrap(wrappedClass);
    }

    @Override
    public void startContext(Class<? extends Annotation> scope) {
        delegate.startContext(scope);
    }

    @Override
    public void stopContext(Class<? extends Annotation> scope) {
        delegate.stopContext(scope);
    }

    @Override
    public void pause() {
        delegate.pause();
    }

    @Override
    public void resume() {
        delegate.resume();
    }

    @Override
    public void setCdiClassLoaderBuilder(CdiClassLoaderBuilder builder) {
        this.builder = builder;
    }

}
