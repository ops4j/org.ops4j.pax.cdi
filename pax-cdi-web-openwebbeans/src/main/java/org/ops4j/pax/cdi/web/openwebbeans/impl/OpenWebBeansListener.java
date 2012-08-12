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

import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.webbeans.config.WebBeansContext;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.Injector;

public class OpenWebBeansListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        CdiContainer cdiContainer = (CdiContainer) context
            .getAttribute("org.ops4j.pax.cdi.container");

        WebBeansContext webBeansContext = cdiContainer.unwrap(WebBeansContext.class);
        BeanManager manager = webBeansContext.getBeanManagerImpl();

        Injector injector = new Injector(manager);
        context.setAttribute(JettyDecorator.INJECTOR_KEY, injector);
        JettyDecorator.register(context);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        context.removeAttribute("org.ops4j.pax.cdi.container");
    }
}
