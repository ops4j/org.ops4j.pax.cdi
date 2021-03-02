/*
 * Copyright 2021 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.api.event;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;

/**
 * Wraps an OSGi {@link BundleEvent} and the originating {@link Bundle} in a CDI event for CDI
 * observer methods.
 * <p>
 * The underlying event source is a BundleTracker. The {@code BundleEvent} may be null if the given
 * bundle changed state before the BundleTracker was opened.
 * 
 * @author Harald Wellmann
 * 
 */
public class BundleCdiEvent {

    private Bundle bundle;

    private BundleEvent bundleEvent;

    /**
     * Constructs a CDI bundle event for a given OSGi bundle and an event related to this bundle.
     * 
     * @param bundle
     *            OSGi bundle triggering this event
     * @param bundleEvent
     *            OSGi bundle event triggered by the given bundle, possibly null to indicate that
     *            the given bundle was present before the receiving bundle was started.
     */
    public BundleCdiEvent(Bundle bundle, BundleEvent bundleEvent) {
        this.bundle = bundle;
        this.bundleEvent = bundleEvent;
    }

    /**
     * @return the bundle
     */
    public Bundle getBundle() {
        return bundle;
    }

    /**
     * @return the bundleEvent
     */
    public BundleEvent getBundleEvent() {
        return bundleEvent;
    }
}
