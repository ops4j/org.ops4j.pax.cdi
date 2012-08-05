package org.ops4j.pax.cdi.samples.jetty;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;


public class ContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ClassLoader classLoader = getClass().getClassLoader();
//        try {
//            //classLoader.loadClass("org.eclipse.jetty.servlet.ServletContextHandler");
//        }
//        catch (ClassNotFoundException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // TODO Auto-generated method stub
        
    }

}
