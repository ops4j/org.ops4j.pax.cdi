package org.ops4j.pax.cdi.web;

import javax.servlet.ServletContextListener;

import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.web.impl.CdiWebAppDependencyManager;
import org.ops4j.pax.swissbox.lifecycle.AbstractLifecycle;
import org.ops4j.pax.swissbox.tracker.ReplaceableService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public abstract class AbstractWebAdapter extends AbstractLifecycle {

	protected BundleContext bundleContext;
	private ServiceTracker<CdiContainer, CdiContainer> cdiContainerTracker;
	private CdiWebAppDependencyManager dependencyManager;
	private ReplaceableService<HttpService> httpService;

	protected AbstractWebAdapter(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	@Override
	protected void onStart() {
		cdiContainerTracker = new ServiceTracker<CdiContainer, CdiContainer>(bundleContext,
			CdiContainer.class, new CdiContainerListener());
		cdiContainerTracker.open();

		dependencyManager = new CdiWebAppDependencyManager(bundleContext, getServletContextListener());

		httpService = new ReplaceableService<HttpService>(bundleContext, HttpService.class, dependencyManager);
		httpService.start();
	}

	protected abstract ServletContextListener getServletContextListener();

	@Override
	protected void onStop() {
		httpService.stop();
		cdiContainerTracker.close();
	}

	private class CdiContainerListener implements
		ServiceTrackerCustomizer<CdiContainer, CdiContainer> {

		@Override
		public CdiContainer addingService(ServiceReference<CdiContainer> reference) {
			CdiContainer cdiContainer = bundleContext.getService(reference);
			dependencyManager.addCdiContainer(cdiContainer);
			return cdiContainer;
		}

		@Override
		public void modifiedService(ServiceReference<CdiContainer> reference, CdiContainer service) {
			dependencyManager.removeCdiContainer(service);
			CdiContainer cdiContainer = bundleContext.getService(reference);
			dependencyManager.addCdiContainer(cdiContainer);
		}

		@Override
		public void removedService(ServiceReference<CdiContainer> reference, CdiContainer service) {
			dependencyManager.removeCdiContainer(service);
		}
	}
}
