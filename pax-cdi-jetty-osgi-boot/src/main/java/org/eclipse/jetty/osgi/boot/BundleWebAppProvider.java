//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.osgi.boot;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletContainerInitializer;

import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.osgi.boot.internal.serverfactory.ServerInstanceWrapper;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.ops4j.pax.cdi.spi.BeanBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * BundleWebAppProvider
 *
 * A Jetty Provider that knows how to deploy a WebApp contained inside a Bundle.
 * 
 */
public class BundleWebAppProvider extends AbstractWebAppProvider implements BundleProvider,
    ServiceTrackerCustomizer<ServletContainerInitializer, ServletContainerInitializer> {

    private static final Logger LOG = Log.getLogger(AbstractWebAppProvider.class);
    
    /**
     * Map of Bundle to App. Used when a Bundle contains a webapp.
     */

    private Map<Bundle, BundleInfo> bundleInfoMap = new HashMap<Bundle, BundleInfo>();

    private ServiceRegistration _serviceRegForBundles;

    private ServiceTracker<ServletContainerInitializer, ServletContainerInitializer> st;

    private BundleContext bundleContext;

    /* ------------------------------------------------------------ */
    /**
     * @param wrapper
     */
    public BundleWebAppProvider (ServerInstanceWrapper wrapper)
    {
        super(wrapper);
    }
    
    
    
    
    /* ------------------------------------------------------------ */
    /** 
     * @see org.eclipse.jetty.util.component.AbstractLifeCycle#doStart()
     */
    protected void doStart() throws Exception
    {
        //register as an osgi service for deploying bundles, advertising the name of the jetty Server instance we are related to
        Dictionary<String,String> properties = new Hashtable<String,String>();
        properties.put(OSGiServerConstants.MANAGED_JETTY_SERVER_NAME, getServerInstanceWrapper().getManagedServerName());
        _serviceRegForBundles = FrameworkUtil.getBundle(this.getClass()).getBundleContext().registerService(BundleProvider.class.getName(), this, properties);
        bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        _serviceRegForBundles = bundleContext.registerService(BundleProvider.class.getName(), this,
            properties);
        Filter filter = bundleContext
            .createFilter("(&(objectClass=javax.servlet.ServletContainerInitializer)(org.ops4j.pax.cdi.bundle.id=*))");
        st = new ServiceTracker<ServletContainerInitializer, ServletContainerInitializer>(
            bundleContext, filter, this);
        st.open();
        super.doStart();
    }

    /* ------------------------------------------------------------ */
    /** 
     * @see org.eclipse.jetty.util.component.AbstractLifeCycle#doStop()
     */
    @Override
    protected void doStop() throws Exception
    {
        st.close();
        //unregister ourselves
        if (_serviceRegForBundles != null)
        {
            try
            {
                _serviceRegForBundles.unregister();
            }
            catch (Exception e)
            {
                LOG.warn(e);
            }
        }
     
        super.doStop();
    }
    
    
    

    
    
    /* ------------------------------------------------------------ */
    /**
     * A bundle has been added that could be a webapp 
     * @param bundle
     */
    public synchronized boolean bundleAdded (Bundle bundle) throws Exception
    {
        if (bundle == null)
            return false;

        BundleInfo bundleInfo = bundleInfoMap.get(bundle);
        if (bundleInfo == null) {
            bundleInfo = new BundleInfo();
            bundleInfo.setStarted(true);
            bundleInfoMap.put(bundle, bundleInfo);
        }
        return processDeployment(bundle, true);        
    }

    
    /* ------------------------------------------------------------ */
    /** 
     * Bundle has been removed. If it was a webapp we deployed, undeploy it.
     * @param bundle
     * 
     * @return true if this was a webapp we had deployed, false otherwise
     */
    public synchronized boolean bundleRemoved (Bundle bundle) throws Exception
    {
        BundleInfo bundleInfo = bundleInfoMap.remove(bundle);
        if (bundleInfo == null) {
            return false;
        }
        App app = bundleInfo.getApp();
        if (app != null)
        {
            getDeploymentManager().removeApp(app); 
            return true;
        }
        return false;
    }
    
   

    
    
    /* ------------------------------------------------------------ */
    private static String getContextPath(Bundle bundle)
    {
        Dictionary<?, ?> headers = bundle.getHeaders();
        String contextPath = (String) headers.get(OSGiWebappConstants.RFC66_WEB_CONTEXTPATH);
        if (contextPath == null)
        {
            // extract from the last token of the bundle's location:
            // (really ?could consider processing the symbolic name as an alternative
            // the location will often reflect the version.
            // maybe this is relevant when the file is a war)
            String location = bundle.getLocation();
            String toks[] = location.replace('\\', '/').split("/");
            contextPath = toks[toks.length - 1];
            // remove .jar, .war etc:
            int lastDot = contextPath.lastIndexOf('.');
            if (lastDot != -1)
                contextPath = contextPath.substring(0, lastDot);
        }
        if (!contextPath.startsWith("/"))
            contextPath = "/" + contextPath;
 
        return contextPath;
    }
    
    @Override
    public synchronized ServletContainerInitializer addingService(
        ServiceReference<ServletContainerInitializer> reference) {
        ServletContainerInitializer initializer = bundleContext.getService(reference);
        Bundle bundle = reference.getBundle();
        BundleInfo bundleInfo = bundleInfoMap.get(bundle);
        if (bundleInfo == null) {
            bundleInfo = new BundleInfo();
            bundleInfoMap.put(bundle, bundleInfo);
        }
        bundleInfo.setInitializer(initializer);
        processDeployment(bundle, false);
        return initializer;
    }

    private boolean processDeployment(Bundle bundle, boolean bundleAdded) {

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getServerInstanceWrapper().getParentClassLoaderForWebapps());
        BundleInfo bundleInfo = bundleInfoMap.get(bundle);
        OSGiApp app = null;
        boolean bundleStarted = bundleAdded;
        if (bundleInfo != null) {
            app = (OSGiApp) bundleInfo.getApp();
            bundleStarted |= bundleInfo.isStarted();
        }

        
        try 
        {
            if (bundleStarted && app == null) {
                app = createApp(bundle);
                if (app == null) 
                {
                    return false;
                }
                if (bundleInfo == null) 
                {
                    bundleInfo = new BundleInfo();
                    bundleInfoMap.put(bundle, bundleInfo);
                }
                bundleInfo.setApp(app);
                bundleInfo.setStarted(true);
            }

            if (BeanBundles.isBeanBundle(bundle)) 
            {
                ServletContainerInitializer initializer = bundleInfo.getInitializer();
                if (app != null && initializer != null) 
                {
                    try 
                    {
                        app.getContextHandler().setAttribute("org.ops4j.pax.cdi.initializer",
                            initializer);
                        getDeploymentManager().addApp(app);
                    }
                    catch (Exception e) 
                    {
                        LOG.warn(e);
                    }
                }
            }
            else 
            {
                if (app != null) 
                {
                    getDeploymentManager().addApp(app);
                }                
            }
            return true;
        }
        catch (Exception e)
        {            
            throw new RuntimeException(e);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }




    private OSGiApp createApp(Bundle bundle) {
        OSGiApp app;
        String base = null;
        String contextPath = null;
        String originId = null;
        Dictionary headers = bundle.getHeaders();

        //does the bundle have a OSGiWebappConstants.JETTY_WAR_FOLDER_PATH 
        if (headers.get(OSGiWebappConstants.JETTY_WAR_FOLDER_PATH) != null)
        {
            base = (String)headers.get(OSGiWebappConstants.JETTY_WAR_FOLDER_PATH);
            contextPath = getContextPath(bundle);
            originId = getOriginId(bundle, base);
        }

        //does the bundle have a WEB-INF/web.xml
        else if (bundle.getEntry("/WEB-INF/web.xml") != null)
        {
            base = ".";
            contextPath = getContextPath(bundle);
            originId = getOriginId(bundle, base);
        }

        //does the bundle define a OSGiWebappConstants.RFC66_WEB_CONTEXTPATH
        else if (headers.get(OSGiWebappConstants.RFC66_WEB_CONTEXTPATH) != null)
        {
            //Could be a static webapp with no web.xml
            base = ".";
            contextPath = (String)headers.get(OSGiWebappConstants.RFC66_WEB_CONTEXTPATH);
            originId = getOriginId(bundle,base);
        }

        if (contextPath == null) {
            return null;
        }

        //TODO : we don't know whether an app is actually deployed, as deploymentManager swallows all
        //exceptions inside the impl of addApp. Need to send the Event and also register as a service
        //only if the deployment succeeded
        app = new OSGiApp(getDeploymentManager(), this, bundle, originId);
        app.setWebAppPath(base);
        app.setContextPath(contextPath);
        return app;
    }

    
    @Override
    public void modifiedService(ServiceReference<ServletContainerInitializer> reference,
        ServletContainerInitializer service) 
    {
        // not used
    }

    @Override
    public synchronized void removedService(ServiceReference<ServletContainerInitializer> reference,
        ServletContainerInitializer service) 
    {
        Bundle bundle = reference.getBundle();
        BundleInfo bundleInfo = bundleInfoMap.get(bundle);
        App app = bundleInfo.getApp();
        if (app != null) 
        {
            bundleInfo.setApp(null);
            try 
            {
                getDeploymentManager().removeApp(app); 
            }
            catch (Exception e) 
            {
                LOG.warn(e);
            }
        }        
    }
}
