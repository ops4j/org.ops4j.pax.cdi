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
package org.ops4j.pax.cdi.undertow.weld.impl;

import javax.servlet.ServletContainerInitializer;

import org.ops4j.pax.cdi.servlet.AbstractWebCdiContainerListener;
import org.ops4j.pax.cdi.spi.CdiContainer;

/**
 * Pax CDI Adapter for Weld using the Undertow servlet container. This adapter provides
 * a servlet container initializer to be picked up by Undertow.
 * 
 * @author Harald Wellmann
 *
 */
public class WeldUndertowAdapter extends AbstractWebCdiContainerListener {

    /**
     * Provides a servlet container initializer for the given CDI container.
     * @param cdiContainer CDI container of bean bundle
     * @return servlet container initializier
     */
    @Override
    protected ServletContainerInitializer getServletContainerInitializer(CdiContainer cdiContainer) {
        ServletContainerInitializer initializer = new WeldServletContainerInitializer(
            cdiContainer, new WeldServletContextListener());
        return initializer;
    }
}
