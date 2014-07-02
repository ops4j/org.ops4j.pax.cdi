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

import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.jboss.weld.Container;
import org.ops4j.pax.cdi.jetty.CdiServletContainerInitializer;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.osgi.framework.Bundle;


public class WeldServletContainerInitializer extends CdiServletContainerInitializer {

    public WeldServletContainerInitializer(CdiContainer cdiContainer,
        ServletContextListener servletContextListener) {
        super(cdiContainer, servletContextListener);
    }
    
    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext ctx) throws ServletException {
        super.onStartup(classes, ctx);
        Bundle bundle = cdiContainer.getBundle();
        String contextId = String.format("%s:%d", bundle.getSymbolicName(), bundle.getBundleId()); 
        ctx.setInitParameter(Container.CONTEXT_ID_KEY, contextId);
    }

}
