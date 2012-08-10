package org.ops4j.pax.cdi.web.impl;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.ops4j.pax.cdi.spi.CdiContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdiServletContainerInitializer implements ServletContainerInitializer {

    private static Logger logger = LoggerFactory.getLogger(CdiServletContainerInitializer.class);

    private CdiContainer cdiContainer;

	private ServletContextListener servletContextListener;

    public CdiServletContainerInitializer(CdiContainer cdiContainer, ServletContextListener servletContextListener) {
        this.cdiContainer = cdiContainer;
        this.servletContextListener = servletContextListener;
    }

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext ctx) throws ServletException {
        logger.info("storing CdiContainer in ServletContext for [{}]", cdiContainer.getBundle());
        ctx.setAttribute("org.ops4j.pax.cdi.container", cdiContainer);
        ctx.addListener(servletContextListener);
    }

}
