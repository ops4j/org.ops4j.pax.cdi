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
import javax.servlet.ServletContextEvent;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;

import org.jboss.weld.el.WeldELContextListener;
import org.jboss.weld.manager.api.WeldManager;
import org.jboss.weld.servlet.WeldInitialListener;
import org.jboss.weld.servlet.api.ServletListener;
import org.jboss.weld.servlet.api.helpers.ForwardingServletListener;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet context listener for starting and stopping the Weld CDI container.
 *
 * @author Harald Wellmann
 *
 */
public class WeldServletContextListener extends ForwardingServletListener {

    private static Logger log = LoggerFactory.getLogger(WeldServletContextListener.class);

    private ServletListener weldListener;

    private CdiContainer cdiContainer;

    /**
     * Creates a listener.
     */
    public WeldServletContextListener() {
        weldListener = new WeldInitialListener();
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        ServletContext context = sce.getServletContext();
        cdiContainer = (CdiContainer) context
            .getAttribute("org.ops4j.pax.cdi.container");
        cdiContainer.start(context);
        WeldManager manager = cdiContainer.unwrap(WeldManager.class);

        Injector injector = new Injector(cdiContainer);
        context.setAttribute(JettyDecorator.INJECTOR_KEY, injector);
        JettyDecorator.process(context);
        log.info("registered Jetty decorator for JSR-299 injection");

        JspFactory jspFactory = JspFactory.getDefaultFactory();
        if (jspFactory != null) {
            JspApplicationContext jspApplicationContext = jspFactory
                .getJspApplicationContext(context);

            jspApplicationContext.addELResolver(manager.getELResolver());
            jspApplicationContext.addELContextListener(new WeldELContextListener());
        }
        super.contextInitialized(sce);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        cdiContainer.stop();
        sce.getServletContext().removeAttribute(JettyDecorator.INJECTOR_KEY);

        super.contextDestroyed(sce);
    }

    @Override
    protected ServletListener delegate() {
        return weldListener;
    }
}
