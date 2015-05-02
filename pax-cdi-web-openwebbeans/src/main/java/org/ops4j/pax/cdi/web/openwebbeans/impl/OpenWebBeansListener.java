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
import org.ops4j.pax.cdi.spi.Injector;
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
    private ContainerLifecycle lifecycle;
    private WebBeansContext webBeansContext;
    private CdiContainer cdiContainer;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        cdiContainer = (CdiContainer) context.getAttribute("org.ops4j.pax.cdi.container");
        cdiContainer.start(context);

        webBeansContext = cdiContainer.unwrap(WebBeansContext.class);
        lifecycle = cdiContainer.unwrap(ContainerLifecycle.class);
        BeanManager manager = webBeansContext.getBeanManagerImpl();

        Injector injector = new Injector(cdiContainer);
        context.setAttribute(JettyDecorator.INJECTOR_KEY, injector);
        JettyDecorator.register(context);

        context.setAttribute(BeanManager.class.getName(), manager);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        context.removeAttribute("org.ops4j.pax.cdi.container");
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
