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
package org.ops4j.pax.cdi.web.openwebbeans.impl;

import java.util.EventListener;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.ops4j.pax.cdi.spi.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator for servlets, filters and listeners instantiated by Jetty. The decorator performs
 * dependency injection and cleanup.
 *
 * @author Harald Wellmann
 *
 */
public class JettyDecorator implements ServletContextHandler.Decorator {

    public static final String INJECTOR_KEY = "org.ops4j.pax.cdi.injector";

    private static Logger log = LoggerFactory.getLogger(JettyDecorator.class);

    private ServletContext servletContext;

    private Injector injector;

    protected JettyDecorator(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Adds an instance of this decorator class to the given context.
     *
     * @param context
     *            servlet context
     */
    public static void register(ServletContext context) {
        if (context instanceof ContextHandler.Context) {
            ContextHandler.Context cc = (ContextHandler.Context) context;
            ContextHandler handler = cc.getContextHandler();
            if (handler instanceof ServletContextHandler) {
                ServletContextHandler sch = (ServletContextHandler) handler;
                sch.addDecorator(new JettyDecorator(context));
                log.info("registered Jetty decorator for JSR-299 injection");
            }
        }
    }

    protected Injector getInjector() {
        if (injector == null) {
            injector = (Injector) servletContext.getAttribute(INJECTOR_KEY);

            if (injector == null) {
                throw new IllegalArgumentException(
                    "no injector found in servlet context attributes");
            }
        }
        return injector;
    }

    @Override
    public <T extends Filter> T decorateFilterInstance(T filter) {
        getInjector().inject(filter);
        return filter;
    }

    @Override
    public <T extends Servlet> T decorateServletInstance(T servlet) {
        getInjector().inject(servlet);
        return servlet;
    }

    @Override
    public <T extends EventListener> T decorateListenerInstance(T listener) {
        getInjector().inject(listener);
        return listener;
    }

    @Override
    public void decorateFilterHolder(FilterHolder filter) {
    }

    @Override
    public void decorateServletHolder(ServletHolder servlet) {
    }

    @Override
    public void destroyServletInstance(Servlet servlet) {
        getInjector().destroy(servlet);
    }

    @Override
    public void destroyFilterInstance(Filter filter) {
        getInjector().destroy(filter);
    }

    @Override
    public void destroyListenerInstance(EventListener listener) {
        getInjector().destroy(listener);
    }
}
