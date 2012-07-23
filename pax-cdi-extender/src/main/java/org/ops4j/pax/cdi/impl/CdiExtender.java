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
 */
package org.ops4j.pax.cdi.impl;

import static org.ops4j.pax.swissbox.core.ContextClassLoaderUtils.doWithClassLoader;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.spi.BeanManager;

import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.cdi.api.BeanBundle;
import org.ops4j.pax.cdi.api.ContainerInitialized;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.swissbox.framework.ServiceLookup;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdiExtender {
    
    private static Logger log = LoggerFactory.getLogger(CdiExtender.class);

    /** Context of this bundle. */
    private BundleContext bc;

    private CdiContainerFactory containerFactory;
    private CdiExtensionObserver extensionObserver;
    private ScheduledExecutorService executor;
    private ScheduledFuture<Void> afterQuietPeriod;  

    private Callable<Void> createContainersTask = new Callable<Void>() {

        @Override
        public Void call() throws Exception {
            createCdiContainers();
            return null;
        }        
    };
    
    
    private Map<Long, Bundle> toBeCreated = new LinkedHashMap<Long, Bundle>();
    private Map<Long, Bundle> toBeDestroyed = new LinkedHashMap<Long, Bundle>();
    
    private State state = State.QUIET_PERIOD;
    
    enum State {
        QUIET_PERIOD,
        OPERATIONAL
    }

    public CdiExtender(BundleContext bc, CdiExtensionObserver extensionObserver) {
        this.bc = bc;
        this.extensionObserver = extensionObserver;
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(createContainersTask, 2, TimeUnit.SECONDS);
    }
    
    public void createCdiContainers() {
        containerFactory = ServiceLookup.getService(bc, CdiContainerFactory.class);
        containerFactory.setExtensionBundles(extensionObserver.getExtensionBundles());
        List<Bundle> bundles = new ArrayList<Bundle>(toBeCreated.values());
        for (Bundle beanBundle : bundles) {
            createCdiContainer(beanBundle);
        }
        
        synchronized (state) {
            for (Bundle beanBundle : bundles) {
                toBeCreated.remove(beanBundle.getBundleId());
            }
            state = State.OPERATIONAL;
        }
    }
    
    private void createCdiContainer(final Bundle bundle) {
        final CdiContainer container = containerFactory.createContainer(bundle);

        /*
         * Start the CDI container under a suitable thread context class loader so that the CDI
         * implementation will be able to load classes from all required bundles.
         * 
         * TODO Move this to CdiContainer implementation?
         */
        try {
            container.start();
            doWithClassLoader(container.getContextClassLoader(),
                new Callable<ServiceRegistration<CdiContainer>>() {

                    @Override
                    public ServiceRegistration<CdiContainer> call() throws Exception {
                        // set bundle context on BeanBundle CDI bean
                        BeanBundle cdiBundle = container.getInstance().select(BeanBundle.class)
                            .get();
                        BundleContext bc = bundle.getBundleContext();
                        cdiBundle.setBundleContext(bc);

                        // fire ContainerInitialized event
                        BeanManager beanManager = container.getBeanManager();
                        beanManager.fireEvent(new ContainerInitialized());

                        // register CdiContainer service
                        Dictionary<String, Object> props = new Hashtable<String, Object>();
                        props.put("bundleId", bundle.getBundleId());
                        props.put("symbolicName", bundle.getSymbolicName());

                        return bc.registerService(CdiContainer.class, container, props);
                    }
                });
        }
        catch (Exception exc) {
            log.error("", exc);
            throw new Ops4jException(exc);
        }
    }

    private void destroyCdiContainer(final Bundle bundle) {
        CdiContainer container = containerFactory.getContainer(bundle);
        container.stop();
        containerFactory.removeContainer(bundle);
        toBeDestroyed.remove(bundle.getBundleId());
        // TODO remove CdiContainer service registration
    }    
    
    public synchronized void createContainer(final Bundle bundle) {
        toBeCreated.put(bundle.getBundleId(), bundle);
        if (state == State.QUIET_PERIOD) {
            afterQuietPeriod.cancel(false);
            afterQuietPeriod = executor.schedule(createContainersTask, 2, TimeUnit.SECONDS);
        }
        else {
            executor.submit(new Callable<Long>() {

                @Override
                public Long call() throws Exception {
                    createCdiContainer(bundle);
                    return bundle.getBundleId();
                }
                
            });
        }        
    }
    
    public synchronized void destroyContainer(final Bundle bundle) {
        toBeCreated.remove(bundle.getBundleId());
        toBeDestroyed.put(bundle.getBundleId(), bundle);
        if (state == State.OPERATIONAL) {
            executor.submit(new Callable<Long>() {

                @Override
                public Long call() throws Exception {
                    destroyCdiContainer(bundle);
                    return bundle.getBundleId();
                }
                
            });
        }        
        
    }
    
    public void stop() {
        executor.shutdownNow();
    }
    
}
