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
package org.ops4j.pax.cdi.undertow.openwebbeans.impl;

import javax.servlet.ServletContextListener;

import org.ops4j.pax.cdi.servlet.CdiServletContainerInitializer;
import org.ops4j.pax.cdi.spi.CdiContainer;

/**
 * Servlet container initializer for Undertow, integrating Pax CDI and OpenWebBeans.
 *
 * @author Harald Wellmann
 *
 */
public class OpenWebBeansServletContainerInitializer extends CdiServletContainerInitializer {

    /**
     * Creates a servlet container initializer for the given CDI container and the given context
     * listener to be called on startup.
     *
     * @param cdiContainer
     *            CDI container
     * @param servletContextListener
     *            servlet context listener
     */
    public OpenWebBeansServletContainerInitializer(CdiContainer cdiContainer) {
        super(cdiContainer, null);
    }

    @Override
    public ServletContextListener createServletContextListener() {
        return new OpenWebBeansListener();
    }
}
