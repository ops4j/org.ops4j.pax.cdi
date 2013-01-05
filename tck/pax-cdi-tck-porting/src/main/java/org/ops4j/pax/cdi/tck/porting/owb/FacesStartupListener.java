package org.ops4j.pax.cdi.tck.porting.owb;

import javax.servlet.ServletContextEvent;

import org.apache.myfaces.webapp.StartupServletContextListener;


public class FacesStartupListener extends StartupServletContextListener {
    
    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        // ignore
    }
}
