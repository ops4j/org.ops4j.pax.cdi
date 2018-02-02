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
package org.ops4j.pax.cdi.web.impl;

import java.util.Collection;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.ops4j.pax.cdi.spi.CdiClassLoaderBuilderCustomizer;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiClassLoaderBuilder;
import org.ops4j.pax.cdi.web.ServletContextListenerFactory;
import org.ops4j.pax.web.service.spi.util.ResourceDelegatingBundleClassLoader;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.cdi.web.ServletContextListenerFactory.CDI_CONTAINER_ATTRIBUTE;

/**
 * A {@link ServletContainerInitializer} which stores the CDI container in the servlet context and
 * registers a CDI provider dependent {@link ServletContextListener}.
 * <p>
 * This listener is responsible for starting the CDI container.
 *
 * @author Harald Wellmann
 *
 */
public class CdiServletContainerInitializer implements ServletContainerInitializer {

    private static Logger log = LoggerFactory.getLogger(CdiServletContainerInitializer.class);

    private CdiContainer cdiContainer;

    private ServletContextListenerFactory servletContextListener;

    /**
     * Creates a servlet container initializer for the given CDI container and the given context
     * listener to be called on startup.
     *
     * @param cdiContainer
     *            CDI container
     * @param servletContextListener
     *            servlet context listener
     */
    public CdiServletContainerInitializer(CdiContainer cdiContainer,
        ServletContextListenerFactory servletContextListener) {
        this.cdiContainer = cdiContainer;
        this.servletContextListener = servletContextListener;
    }

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext ctx) throws ServletException {
        Bundle bundle = cdiContainer.getBundle();
        log.info("storing CdiContainer in ServletContext for [{}]", bundle);

        // FIXME refactor and move to Weld adapter bundle
        String contextId = String.format("%s:%d", bundle.getSymbolicName(), bundle.getBundleId());
        ctx.setInitParameter("WELD_CONTEXT_ID_KEY", contextId);

        CdiClassLoaderBuilderCustomizer customizer = cdiContainer.unwrap(CdiClassLoaderBuilderCustomizer.class);
        if (customizer != null) {
            customizer.setCdiClassLoaderBuilder(new PaxWebClassLoaderBuilder());
        }

        ctx.setAttribute(CDI_CONTAINER_ATTRIBUTE, cdiContainer);
        ctx.addListener(servletContextListener.createServletContextListener());
    }

    /**
     * In pax-web environment, we want to use already created "resource delegating" class loader
     */
    private static class PaxWebClassLoaderBuilder implements CdiClassLoaderBuilder {
        @Override
        public ClassLoader buildContextClassLoader(Object environment, Bundle beanArchive, Collection<Bundle> extensionBundles, Collection<Bundle> additionalBundles) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl instanceof ResourceDelegatingBundleClassLoader) {
                additionalBundles.forEach(((ResourceDelegatingBundleClassLoader) cl)::addBundle);
                return cl;
            }
            return null;
        }
    }

}
