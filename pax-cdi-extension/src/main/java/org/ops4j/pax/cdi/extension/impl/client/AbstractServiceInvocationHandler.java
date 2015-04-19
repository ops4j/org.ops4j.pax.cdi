package org.ops4j.pax.cdi.extension.impl.client;

import java.lang.reflect.InvocationHandler;

import javax.enterprise.inject.spi.InjectionPoint;

import org.ops4j.pax.cdi.extension.impl.compat.PrototypeScopeUtils;
import org.ops4j.pax.cdi.extension.impl.compat.ServiceObjectsWrapper;
import org.ops4j.pax.cdi.extension.impl.util.InjectionPointOsgiUtils;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class AbstractServiceInvocationHandler<S> implements InvocationHandler {
	
	protected InjectionPoint			ip;
	protected BundleContext				bundleContext;
	protected ServiceReference<S>		serviceRef;
	protected CdiContainer				cdiContainer;
	protected ServiceObjectsWrapper<S>	serviceObjects;
	
	@SuppressWarnings("unchecked")
	protected AbstractServiceInvocationHandler(InjectionPoint ip) {
		this.ip = ip;
		this.bundleContext = InjectionPointOsgiUtils.getBundleContext(ip);
		this.serviceRef = InjectionPointOsgiUtils.getServiceReference(ip);
		this.serviceObjects = PrototypeScopeUtils.createServiceObjectsWrapper(this.bundleContext, this.serviceRef);
		ServiceReference<CdiContainerFactory> serviceReference = this.bundleContext
				.getServiceReference(CdiContainerFactory.class);
		CdiContainerFactory cdiContainerFactory = this.bundleContext.getService(serviceReference);
		this.cdiContainer = cdiContainerFactory.getContainer(this.bundleContext.getBundle());
	}
	
	public abstract void release();
	
}
