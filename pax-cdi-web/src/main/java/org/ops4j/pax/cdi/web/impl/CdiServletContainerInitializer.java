package org.ops4j.pax.cdi.web.impl;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.ops4j.pax.cdi.spi.CdiContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdiServletContainerInitializer implements ServletContainerInitializer {

    private static Logger logger = LoggerFactory.getLogger(CdiServletContainerInitializer.class);

    private CdiContainer cdiContainer;

    public CdiServletContainerInitializer(CdiContainer cdiContainer) {
        this.cdiContainer = cdiContainer;
    }

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext ctx) throws ServletException {
        // logger.info("customizing ServletContext for {}",
        // cdiContainer.getBundle().getSymbolicName());
        logger.info("customizing ServletContext");

    }

}
