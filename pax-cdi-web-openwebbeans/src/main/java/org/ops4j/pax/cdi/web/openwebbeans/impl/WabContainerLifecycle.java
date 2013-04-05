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
 * 
 * Derived from org.apache.webbeans.web.lifecycle.WebContainerLifecycle.
 */
package org.ops4j.pax.cdi.web.openwebbeans.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.el.ELResolver;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;

import org.apache.webbeans.component.InjectionPointBean;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.ResourceInjectionService;
import org.apache.webbeans.spi.adaptor.ELAdaptor;
import org.apache.webbeans.web.context.WebContextsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WabContainerLifecycle extends AbstractLifeCycle {
    
    private static Logger log = LoggerFactory.getLogger(WabContainerLifecycle.class);

    /** Manages unused conversations */
    private ScheduledExecutorService service;

    /**
     * Creates a new lifecycle instance and initializes the instance variables.
     */
    public WabContainerLifecycle() {
    }

    /**
     * Creates a new lifecycle instance and initializes the instance variables.
     */
    public WabContainerLifecycle(WebBeansContext webBeansContext) {
        super(null, webBeansContext);
    }

    protected void afterStartApplication(final Object startupObject) {
        String strDelay = getWebBeansContext().getOpenWebBeansConfiguration().getProperty(
            OpenWebBeansConfiguration.CONVERSATION_PERIODIC_DELAY, "150000");
        long delay = Long.parseLong(strDelay);

        service = Executors.newScheduledThreadPool(1, new ThreadFactory() {

            public Thread newThread(Runnable runable) {
                Thread t = new Thread(runable, "OwbConversationCleaner-"
                /* + ServletCompatibilityUtil.getServletInfo((ServletContext) (startupObject)) */);
                t.setDaemon(true);
                return t;
            }
        });
        service.scheduleWithFixedDelay(new ConversationCleaner(), delay, delay,
            TimeUnit.MILLISECONDS);

        ELAdaptor elAdaptor = getWebBeansContext().getService(ELAdaptor.class);
        ELResolver resolver = elAdaptor.getOwbELResolver();
        // Application is configured as JSP
        if (getWebBeansContext().getOpenWebBeansConfiguration().isJspApplication()) {
            log.debug("Application is configured as JSP. Adding EL Resolver.");

            JspFactory factory = JspFactory.getDefaultFactory();
            if (factory != null) {
                JspApplicationContext applicationCtx = factory
                    .getJspApplicationContext((ServletContext) (startupObject));
                applicationCtx.addELResolver(resolver);
            }
            else {
                log.debug("could not find default JspFactory instance");
            }
        }

    }

    protected void beforeStartApplication(Object startupObject) {
        setWebBeansContext(WebBeansContext.currentInstance());
        this.scannerService.init(startupObject);
    }

    @Override
    protected void beforeStopApplication(Object stopObject) {
        if (service != null) {
            service.shutdownNow();
        }
    }

    @Override
    protected void afterStopApplication(Object stopObject) {
        // ServletContext servletContext;
        //
        // if(stopObject instanceof ServletContext)
        // {
        // servletContext = (ServletContext)stopObject;
        // }
        // else
        // {
        // //servletContext = getServletContext(stopObject);
        // }

        // Clear the resource injection service
        ResourceInjectionService injectionServices = getWebBeansContext().getService(
            ResourceInjectionService.class);
        if (injectionServices != null) {
            injectionServices.clear();
        }

        // Comment out for commit OWB-502
        // ContextFactory.cleanUpContextFactory();

        this.cleanupShutdownThreadLocals();

        log.debug("OpenWebBeans container has stopped");
    }

    /**
     * Ensures that all ThreadLocals, which could have been set in this (shutdown-) Thread, are
     * removed in order to prevent memory leaks.
     */
    private void cleanupShutdownThreadLocals() {
        InjectionPointBean.removeThreadLocal();
        WebContextsService.removeThreadLocals();
    }

    /**
     * Returns servelt context otherwise throws exception.
     * 
     * @param object
     *            object
     * @return servlet context
     */
    @SuppressWarnings("unused")
    private ServletContext getServletContext(Object object) {
        if (object != null) {
            if (object instanceof ServletContextEvent) {
                ServletContext context = ((ServletContextEvent) object).getServletContext();
                return context;
            }
            else {
                throw new WebBeansException(
                    WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0002));
            }
        }

        throw new IllegalArgumentException("ServletContextEvent object but found null");
    }

    /**
     * Conversation cleaner thread, that clears unused conversations.
     * 
     */
    private static class ConversationCleaner implements Runnable {

        public ConversationCleaner() {

        }

        public void run() {
            WebBeansContext.currentInstance().getConversationManager().destroyWithRespectToTimout();

        }
    }
}
