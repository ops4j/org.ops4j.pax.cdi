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

public class JettyDecorator implements ServletContextHandler.Decorator {

    public static final String INJECTOR_KEY = "org.ops4j.pax.cdi.injector";

    private static Logger log = LoggerFactory.getLogger(JettyDecorator.class);

    private ServletContext servletContext;

    private Injector injector;

    protected JettyDecorator(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

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

    public <T extends Filter> T decorateFilterInstance(T filter) {
        getInjector().inject(filter);
        return filter;
    }

    public <T extends Servlet> T decorateServletInstance(T servlet) {
        getInjector().inject(servlet);
        return servlet;
    }

    public <T extends EventListener> T decorateListenerInstance(T listener) {
        getInjector().inject(listener);
        return listener;
    }

    public void decorateFilterHolder(FilterHolder filter) {
    }

    public void decorateServletHolder(ServletHolder servlet) {
    }

    public void destroyServletInstance(Servlet servlet) {
        getInjector().destroy(servlet);
    }

    public void destroyFilterInstance(Filter filter) {
        getInjector().destroy(filter);
    }

    public void destroyListenerInstance(EventListener listener) {
        getInjector().destroy(listener);
    }
}
