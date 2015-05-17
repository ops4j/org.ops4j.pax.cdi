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

import java.util.Map;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.el.ELContextStore;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet context listener for starting and stopping the OpenWebBeans CDI container.
 *
 * @author Harald Wellmann
 *
 */
public class OpenWebBeansListener implements ServletContextListener, ServletRequestListener,
    HttpSessionListener {


    private static Logger log = LoggerFactory.getLogger(OpenWebBeansListener.class);

    private static final String CDI_CONTAINER = "org.ops4j.pax.cdi.container";

    private ContainerLifecycle lifecycle;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        CdiContainer cdiContainer = (CdiContainer) context.getAttribute(CDI_CONTAINER);
        try {
            cdiContainer.start(context);
        }
        // CHECKSTYLE:SKIP - this exception will not be logged otherwise
        catch (Exception exc) {
            log.error("cannot start OpenWebBeans CDI container", exc);
            throw exc;
        }

        WebBeansContext webBeansContext = cdiContainer.unwrap(WebBeansContext.class);
        lifecycle = cdiContainer.unwrap(ContainerLifecycle.class);
        BeanManager manager = webBeansContext.getBeanManagerImpl();

        CdiInstanceFactoryBuilder builder = new CdiInstanceFactoryBuilder(manager);
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = (Map<String, Object>) context
            .getAttribute("org.ops4j.pax.web.attributes");
        if (attributes != null) {
            attributes.put("org.ops4j.pax.cdi.ClassIntrospecter", builder);
            log.info("registered CdiInstanceFactoryBuilder for Undertow");
        }
        context.setAttribute("org.ops4j.pax.cdi.BeanManager", cdiContainer.getBeanManager());

        context.setAttribute(BeanManager.class.getName(), manager);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        context.removeAttribute(CDI_CONTAINER);
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        log.debug("session created");
        lifecycle.getContextService().startContext(SessionScoped.class, event.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        log.debug("session destroyed");
        ContextsService contextsService = lifecycle.getContextService();
        contextsService.endContext(SessionScoped.class, event.getSession());
        contextsService.endContext(ConversationScoped.class, event.getSession());
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        log.debug("request destroyed");
        ELContextStore elStore = ELContextStore.getInstance(false);
        if (elStore != null) {
            elStore.destroyELContextStore();
        }

        lifecycle.getContextService().endContext(RequestScoped.class, event);

        WabContextsService.removeThreadLocals();
    }

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        log.debug("request initialized");

        lifecycle.getContextService().startContext(RequestScoped.class, event);
    }
}
