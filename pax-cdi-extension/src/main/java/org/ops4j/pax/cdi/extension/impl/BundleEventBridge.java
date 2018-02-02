/*
 * Copyright 2013 Harald Wellmann
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

package org.ops4j.pax.cdi.extension.impl;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import org.ops4j.pax.cdi.api.event.BundleCdiEvent;
import org.ops4j.pax.cdi.api.event.BundleStarted;
import org.ops4j.pax.cdi.api.event.BundleStopped;
import org.ops4j.pax.cdi.spi.BeanBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps OSGi bundle events in as {@link BundleCdiEvent} and fires them as CDI events.
 *
 * @author Harald Wellmann
 */
@ApplicationScoped
public class BundleEventBridge implements BundleTrackerCustomizer<Void> {

    private static Logger log = LoggerFactory.getLogger(BundleEventBridge.class);

    @Inject
    private BundleContext bundleContext;

    @Inject
    private Event<BundleCdiEvent> event;

    private BundleTracker<Void> bundleTracker;

    /**
     * Starts the bundle tracker.
     */
    @PostConstruct
    public void start() {
        int stateMask = Bundle.INSTALLED | Bundle.UNINSTALLED | Bundle.STARTING
            | Bundle.STOPPING | Bundle.RESOLVED | Bundle.ACTIVE;
        bundleTracker = new BundleTracker<>(bundleContext, stateMask, this);
        bundleTracker.open();
    }

    /**
     * Stops the bundle tracker.
     */
    @PreDestroy
    public void stop() {
        bundleTracker.close();
    }

    @Override
    public Void addingBundle(Bundle bundle, BundleEvent bundleEvent) {
        if (!BeanBundles.isActiveBeanBundle(bundle)) {
            return null;
        }
        log.debug("adding bundle {} {}", bundle);
        Event<BundleCdiEvent> childEvent = select(bundleEvent);
        childEvent.fire(new BundleCdiEvent(bundle, bundleEvent));
        return null;
    }

    /**
     * @param bundleEvent
     * @return
     */
    @SuppressWarnings("serial")
    private Event<BundleCdiEvent> select(BundleEvent bundleEvent) {
        if (bundleEvent != null) {
            switch (bundleEvent.getType()) {
                case BundleEvent.STARTED:
                    return event.select(new AnnotationLiteral<BundleStarted>() {
                    });

                case BundleEvent.STOPPED:
                    return event.select(new AnnotationLiteral<BundleStopped>() {
                    });

                default:
                    return event;
            }
        }
        else {
            return event;
        }
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent bundleEvent, Void object) {
        Event<BundleCdiEvent> childEvent = select(bundleEvent);
        childEvent.fire(new BundleCdiEvent(bundle, bundleEvent));
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent bundleEvent, Void object) {
        Event<BundleCdiEvent> childEvent = select(bundleEvent);
        childEvent.fire(new BundleCdiEvent(bundle, bundleEvent));
    }

    // Force the instantation of this bean
    public void applicationScopeInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
    }

}
