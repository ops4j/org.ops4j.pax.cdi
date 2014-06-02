/*
 * Copyright 2014 Harald Wellmann.
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
package org.ops4j.pax.cdi.jetty.openwebbeans.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.servlet.ServletContext;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.ops4j.pax.cdi.spi.CdiContainer;
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
                JettyDecorator decorator = new JettyDecorator(context);
                sch.addDecorator(decorator);
                decorator.adaptContextClassLoader(context, handler);
                log.info("registered Jetty decorator for JSR-299 injection");
            }
        }
    }
    
    private void adaptContextClassLoader(ServletContext context, ContextHandler handler) {
        try {
            if (handler.getClass().getName().equals("org.eclipse.jetty.webapp.WebAppContext")) {
                Method getClassLoader = handler.getClass().getMethod("getClassLoader");
                if (getClassLoader == null) {
                    return;
                }
                Object classLoader = getClassLoader.invoke(handler);
                if (!classLoader.getClass().getName()
                    .equals("org.eclipse.jetty.osgi.boot.internal.webapp.OSGiWebappClassLoader")) {
                    return;
                }
                Field bundleClassLoader = classLoader.getClass().getDeclaredField(
                    "_osgiBundleClassLoader");
                bundleClassLoader.setAccessible(true);
                CdiContainer cdiContainer = (CdiContainer) context
                    .getAttribute("org.ops4j.pax.cdi.container");

                bundleClassLoader.set(classLoader, cdiContainer.getContextClassLoader());
            }
        }
        catch (ReflectiveOperationException exc) {
            log.error("cannot adapt context class loader", exc);
        }
        catch (SecurityException exc) {
            log.error("cannot adapt context class loader", exc);
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
    public <T> T decorate(T o) {
        getInjector().inject(o);
        return o;
    }

    @Override
    public void destroy(Object o) {
        getInjector().destroy(o);
    }   
}
