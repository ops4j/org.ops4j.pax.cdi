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
package org.ops4j.pax.cdi.servlet;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.ops4j.pax.cdi.spi.CdiContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ServletContainerInitializer} which stores the CDI container in the servlet context
 * and registers a CDI provider dependent {@link ServletContextListener}.
 * <p>
 * This listener is responsible for starting the CDI container.
 * 
 * @author Harald Wellmann
 *
 */
public class CdiServletContainerInitializer implements ServletContainerInitializer {

    private static Logger log = LoggerFactory.getLogger(CdiServletContainerInitializer.class);

    protected CdiContainer cdiContainer;

    protected ServletContextListener servletContextListener;

    public CdiServletContainerInitializer(CdiContainer cdiContainer,
        ServletContextListener servletContextListener) {
        this.cdiContainer = cdiContainer;
        this.servletContextListener = servletContextListener;
    }

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext ctx) throws ServletException {
        log.info("storing CdiContainer in ServletContext for [{}]", cdiContainer.getBundle());
        ctx.setAttribute("org.ops4j.pax.cdi.container", cdiContainer);
        ctx.addListener(servletContextListener);
    }
}
