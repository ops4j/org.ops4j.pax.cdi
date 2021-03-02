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

import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;

import org.jboss.weld.Container;
import org.jboss.weld.el.WeldELContextListener;
import org.jboss.weld.manager.api.WeldManager;
import org.jboss.weld.servlet.WeldInitialListener;
import org.jboss.weld.servlet.api.ServletListener;
import org.jboss.weld.servlet.api.helpers.ForwardingServletListener;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.cdi.web.ServletContextListenerFactory.CDI_CONTAINER_ATTRIBUTE;

/**
 * Servlet context listener which starts the CDI container based on Weld, once the servlet context
 * is ready.
 *
 * @author Harald Wellmann
 *
 */
public class WeldServletContextListener extends ForwardingServletListener {

    private static Logger log = LoggerFactory.getLogger(WeldServletContextListener.class);

    private ServletListener weldListener;

    private CdiContainer cdiContainer;

    /**
     * Creates a servlet context listener for Weld.
     */
    public WeldServletContextListener() {
        weldListener = new WeldInitialListener();
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        ServletContext context = sce.getServletContext();
        cdiContainer = (CdiContainer) context.getAttribute(CDI_CONTAINER_ATTRIBUTE);

        Bundle bundle = cdiContainer.getBundle();
        String contextId = String.format("%s:%d", bundle.getSymbolicName(), bundle.getBundleId());
        context.setInitParameter(Container.CONTEXT_ID_KEY, contextId);

        cdiContainer.start(context);
        WeldManager manager = cdiContainer.unwrap(WeldManager.class);

        CdiInstanceFactoryBuilder builder = new CdiInstanceFactoryBuilder(manager);
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = (Map<String, Object>) context
            .getAttribute("org.ops4j.pax.web.attributes");
        if (attributes != null) {
            attributes.put("org.ops4j.pax.cdi.ClassIntrospecter", builder);
            log.info("registered CdiInstanceFactoryBuilder for Undertow");
        }
        context.setAttribute("org.ops4j.pax.cdi.BeanManager", cdiContainer.getBeanManager());

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
        ServletContext context = sce.getServletContext();
        context.removeAttribute("org.ops4j.pax.cdi.ClassIntrospecter");
        context.removeAttribute("org.ops4j.pax.cdi.BeanManager");
        super.contextDestroyed(sce);
    }

    @Override
    protected ServletListener delegate() {
        return weldListener;
    }
}
