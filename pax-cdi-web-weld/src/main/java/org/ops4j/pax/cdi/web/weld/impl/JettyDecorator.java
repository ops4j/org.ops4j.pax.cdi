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
package org.ops4j.pax.cdi.web.weld.impl;

import javax.servlet.ServletContext;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.Decorator;
import org.ops4j.pax.cdi.spi.Injector;

/**
 * Decorator for servlets, filters and listeners instantiated by Jetty. The decorator performs
 * dependency injection and cleanup.
 *
 * @author Harald Wellmann
 *
 */
public class JettyDecorator implements Decorator {

    public static final String INJECTOR_KEY = "org.ops4j.pax.cdi.injector";

    private ServletContext servletContext;
    private Injector injector;

    private JettyDecorator(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Adds an instance of this decorator class to the given context.
     *
     * @param context
     *            servlet context
     */
    public static void process(ServletContext context) {
        if (context instanceof ContextHandler.Context) {
            ContextHandler.Context cc = (ContextHandler.Context) context;
            ContextHandler handler = cc.getContextHandler();
            if (handler instanceof ServletContextHandler) {
                ServletContextHandler sch = (ServletContextHandler) handler;
                sch.getObjectFactory().addDecorator(new JettyDecorator(context));
            }
        }
    }

    private Injector getInjector() {
        if (injector == null) {
            injector = (Injector) servletContext.getAttribute(INJECTOR_KEY);

            if (injector == null) {
                throw new IllegalArgumentException("no injector found in servlet context attributes");
            }
        }
        return injector;
    }

    public <T> T decorate(T target) {
        getInjector().inject(target);
        return target;
    }

    public void destroy(Object target) {
        getInjector().destroy(target);
    }
}
