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
package org.ops4j.pax.cdi.extender.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.cdi.spi.CdiContainerType;
import org.ops4j.pax.swissbox.framework.ServiceLookup;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
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
            try {
                createCdiContainers();
                return null;
            }
            // CHECKSTYLE:SKIP
            catch (Throwable t) {
                log.error("exception in Executor", t);
                throw new Ops4jException(t);
            }
        }
    };

    private Map<Long, Bundle> toBeCreated = new LinkedHashMap<Long, Bundle>();
    private Map<Long, Bundle> toBeDestroyed = new LinkedHashMap<Long, Bundle>();

    private State state = State.QUIET_PERIOD;

    enum State {
        QUIET_PERIOD, OPERATIONAL
    }

    public CdiExtender(BundleContext bc, CdiExtensionObserver extensionObserver) {
        this.bc = bc;
        this.extensionObserver = extensionObserver;
        executor = Executors.newSingleThreadScheduledExecutor();
        afterQuietPeriod = executor.schedule(createContainersTask, 2, TimeUnit.SECONDS);
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
        // check if this is a web bundle
        Dictionary<String, String> headers = bundle.getHeaders();
        String contextPath = headers.get("Web-ContextPath");
        CdiContainerType containerType = (contextPath == null) ? CdiContainerType.STANDALONE
            : CdiContainerType.WEB;
        
        // create container, but do not start it
        final CdiContainer container = containerFactory.createContainer(bundle, containerType);

        /* Web containers will be started later when the servlet context is available.
         * Standalone containers a started right now.
         */ 
        if (containerType == CdiContainerType.STANDALONE) {
            container.start(null);
        }
    }

    private void destroyCdiContainer(final Bundle bundle) {
        CdiContainer container = containerFactory.getContainer(bundle);
        container.stop();
        containerFactory.removeContainer(bundle);
        toBeDestroyed.remove(bundle.getBundleId());
    }

    public synchronized void createContainer(final Bundle bundle) {
        toBeCreated.put(bundle.getBundleId(), bundle);
        if (state == State.QUIET_PERIOD) {
            // TODO make period configurable
            afterQuietPeriod.cancel(false);
            afterQuietPeriod = executor.schedule(createContainersTask, 2, TimeUnit.SECONDS);
        }
        else {
            executor.submit(new Callable<Long>() {

                @Override
                public Long call() throws Exception {
                    try {
                        createCdiContainer(bundle);
                        return bundle.getBundleId();
                    }
                    // CHECKSTYLE:SKIP
                    catch (Throwable t) {
                        log.error("exception in Executor", t);
                        throw new Ops4jException(t);
                    }
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
                    try {
                        destroyCdiContainer(bundle);
                        return bundle.getBundleId();
                    }
                    // CHECKSTYLE:SKIP
                    catch (Throwable t) {
                        log.error("exception in Executor", t);
                        throw new Ops4jException(t);
                    }
                }
            });
        }
    }

    public void stop() {
        executor.shutdownNow();
    }
}
