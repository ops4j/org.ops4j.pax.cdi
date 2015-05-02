package org.ops4j.pax.cdi.extension.impl.client;

import java.lang.reflect.InvocationHandler;

import javax.enterprise.inject.spi.InjectionPoint;

import org.ops4j.pax.cdi.extension.impl.util.InjectionPointOsgiUtils;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Common base class for static and dynamic service invocation handlers.
 *
 * @author Harald Wellmann
 *
 */
public abstract class AbstractServiceInvocationHandler implements InvocationHandler {

    protected InjectionPoint ip;
    protected BundleContext bundleContext;
    protected CdiContainer cdiContainer;

    /**
     * Constructs an invocation handler for the given injection point.
     *
     * @param ip
     *            OSGi service injection point
     */
    protected AbstractServiceInvocationHandler(InjectionPoint ip) {
        this.ip = ip;
        this.bundleContext = InjectionPointOsgiUtils.getBundleContext(ip);
        ServiceReference<CdiContainerFactory> serviceReference = bundleContext
            .getServiceReference(CdiContainerFactory.class);
        CdiContainerFactory cdiContainerFactory = bundleContext.getService(serviceReference);
        this.cdiContainer = cdiContainerFactory.getContainer(bundleContext.getBundle());
    }

    /**
     * Releases any resources held by this invocation handler.
     */
    public abstract void release();

}
