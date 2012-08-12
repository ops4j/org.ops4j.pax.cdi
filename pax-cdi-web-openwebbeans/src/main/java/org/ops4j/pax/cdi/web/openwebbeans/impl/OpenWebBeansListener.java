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

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.webbeans.component.InjectionPointBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.el.ELContextStore;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.web.context.WebContextsService;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenWebBeansListener implements ServletContextListener, ServletRequestListener, HttpSessionListener {
    
    private static Logger log = LoggerFactory.getLogger(OpenWebBeansListener.class);
    private ContainerLifecycle lifecycle;
    private WebBeansContext webBeansContext;
    private ClassLoader contextClassLoader;
    private CdiContainer cdiContainer;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        cdiContainer = (CdiContainer) context
            .getAttribute("org.ops4j.pax.cdi.container");

        webBeansContext = cdiContainer.unwrap(WebBeansContext.class);
        lifecycle = cdiContainer.unwrap(ContainerLifecycle.class);
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

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        log.info("session created");
        // TODO
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        log.info("session destroyed");
        // TODO
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        log.info("request destroyed");
        ELContextStore elStore = ELContextStore.getInstance(false);
        if (elStore != null)
        {
            elStore.destroyELContextStore();
        }

        lifecycle.getContextService().endContext(RequestScoped.class, event);

        cleanupRequestThreadLocals();
        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
    
    /**
     * Ensures that all ThreadLocals, which could have been set in this
     * requests Thread, are removed in order to prevent memory leaks.
     */
    private void cleanupRequestThreadLocals()
    {
        InjectionPointBean.removeThreadLocal();
        WebContextsService.removeThreadLocals();
    }
    

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        log.info("request initialized");

        contextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cdiContainer.getContextClassLoader());
        lifecycle.getContextService().startContext(RequestScoped.class, event);
    }
}
