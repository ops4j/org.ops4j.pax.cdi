package org.ops4j.pax.cdi.web.impl;

import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.swissbox.tracker.ReplaceableService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {

    private BundleContext bc;
    private ServiceTracker<CdiContainer, CdiContainer> cdiContainerTracker;
    private CdiWebAppDependencyManager dependencyManager;
    private ReplaceableService<HttpService> httpService;

    @Override
    public void start(BundleContext context) throws Exception {
        this.bc = context;

        cdiContainerTracker = new ServiceTracker<CdiContainer, CdiContainer>(bc,
            CdiContainer.class, new CdiContainerListener());
        cdiContainerTracker.open();

        dependencyManager = new CdiWebAppDependencyManager(bc);
        
        httpService = new ReplaceableService<HttpService>(bc, HttpService.class, dependencyManager );
        httpService.start();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        httpService.stop();
        cdiContainerTracker.close();
    }

    private class CdiContainerListener implements
        ServiceTrackerCustomizer<CdiContainer, CdiContainer> {

        @Override
        public CdiContainer addingService(ServiceReference<CdiContainer> reference) {
            CdiContainer cdiContainer = bc.getService(reference);
            dependencyManager.addCdiContainer(cdiContainer);
            return cdiContainer;
        }

        @Override
        public void modifiedService(ServiceReference<CdiContainer> reference, CdiContainer service) {
            dependencyManager.removeCdiContainer(service);
            CdiContainer cdiContainer = bc.getService(reference);
            dependencyManager.addCdiContainer(cdiContainer);
        }

        @Override
        public void removedService(ServiceReference<CdiContainer> reference, CdiContainer service) {
            dependencyManager.removeCdiContainer(service);
        }
    }
}
