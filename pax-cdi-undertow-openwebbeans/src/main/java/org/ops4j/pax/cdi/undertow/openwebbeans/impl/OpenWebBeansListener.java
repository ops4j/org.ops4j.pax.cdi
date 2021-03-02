/*
 * Copyright 2021 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.undertow.openwebbeans.impl;

import java.util.Map;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.el.ELContextStore;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.util.WebBeansUtil;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.cdi.web.ServletContextListenerFactory.CDI_CONTAINER_ATTRIBUTE;

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

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        CdiContainer cdiContainer = (CdiContainer) context.getAttribute(CDI_CONTAINER_ATTRIBUTE);
        try {
            cdiContainer.start(event);
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
        context.setAttribute("org.ops4j.pax.cdi.BeanManager", manager);
        context.setAttribute(BeanManager.class.getName(), manager);
    }

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        try {
            log.debug("Starting a new request : [{}]", event == null ? "null" : event
                .getServletRequest().getRemoteAddr());

            this.lifecycle.getContextService().startContext(RequestScoped.class, event);

            // we don't initialise the Session here but do it lazily if it gets requested
            // the first time. See OWB-457
        }
        // CHECKSTYLE:SKIP
        catch (Exception e) {
            log.error(WebBeansLoggerFacade.constructMessage(OWBLogConst.ERROR_0019,
                event == null ? "null" : event.getServletRequest()));
            WebBeansUtil.throwRuntimeExceptions(e);
        }
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        try {
            log.debug("Starting a session with session id : [{}]", event.getSession().getId());
            this.lifecycle.getContextService()
                .startContext(SessionScoped.class, event.getSession());
        }
        // CHECKSTYLE:SKIP
        catch (Exception e) {
            log.error(WebBeansLoggerFacade.constructMessage(OWBLogConst.ERROR_0020,
                event.getSession()));
            WebBeansUtil.throwRuntimeExceptions(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        context.removeAttribute(CDI_CONTAINER_ATTRIBUTE);

        // just to be sure that we didn't lazily create anything...
        cleanupRequestThreadLocals();
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        log.debug("Destroying a request : [{}]", event == null ? "null" : event.getServletRequest()
            .getRemoteAddr());

        // clean up the EL caches after each request
        ELContextStore elStore = ELContextStore.getInstance(false);
        if (elStore != null) {
            elStore.destroyELContextStore();
        }

        this.lifecycle.getContextService().endContext(RequestScoped.class, event);

        this.cleanupRequestThreadLocals();
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        log.debug("Destroying a session with session id : [{}]", event.getSession().getId());
        boolean mustDestroy = ensureRequestScope();

        this.lifecycle.getContextService().endContext(SessionScoped.class, event.getSession());

        if (mustDestroy) {
            requestDestroyed(null);
        }
    }

    private boolean ensureRequestScope() {
        Context context = this.lifecycle.getContextService().getCurrentContext(RequestScoped.class);

        if (context == null || !context.isActive()) {
            requestInitialized(null);
            return true;
        }
        return false;
    }

    /**
     * Ensures that all ThreadLocals, which could have been set in this requests Thread, are removed
     * in order to prevent memory leaks.
     */
    private void cleanupRequestThreadLocals() {
        ContextsService contextsService = this.lifecycle.getContextService();
        if (contextsService != null) {
            contextsService.removeThreadLocals();
        }
    }
}
