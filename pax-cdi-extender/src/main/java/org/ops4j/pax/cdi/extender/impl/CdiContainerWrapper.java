/*
 * Copyright 2014 Harald Wellmann.
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

import org.ops4j.pax.cdi.spi.CdiContainer;
import org.osgi.framework.Bundle;

/**
 * Wraps a CdiContainer for tracking by the BundleTracker of the CdiExtender.
 * <p>
 * Background: For web bean bundle, the CDI web adapter may not yet be available when the bundle
 * gets tracked, so we cannot create a CDI container. Rather than returning null from the bundle
 * tracker callback, we use this wrapper. This ensures that the removeBundle callback will be
 * called.
 *
 * @author Harald Wellmann
 *
 */
public class CdiContainerWrapper {

    private Bundle bundle;
    private CdiContainer cdiContainer;
    private boolean webBundle;

    /**
     * Construct a wrapper for the given bundle.
     *
     * @param bundle
     *            bean bundle
     */
    public CdiContainerWrapper(Bundle bundle) {
        this.bundle = bundle;
    }

    /**
     * Gets the bundle.
     *
     * @return the bundle
     */
    public Bundle getBundle() {
        return bundle;
    }

    /**
     * Sets the bundle.
     *
     * @param bundle
     *            the bundle to set
     */
    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    /**
     * Gets the cdiContainer.
     *
     * @return the cdiContainer
     */
    public CdiContainer getCdiContainer() {
        return cdiContainer;
    }

    /**
     * Sets the cdiContainer.
     *
     * @param cdiContainer
     *            the cdiContainer to set
     */
    public void setCdiContainer(CdiContainer cdiContainer) {
        this.cdiContainer = cdiContainer;
    }

    /**
     * Checks if this bundle is a web bundle.
     *
     * @return true if this is a web bundle
     */
    public boolean isWebBundle() {
        return webBundle;
    }

    /**
     * Sets the web bundle flag.
     *
     * @param webBundle
     *            true if this is a web bundle
     */
    public void setWebBundle(boolean webBundle) {
        this.webBundle = webBundle;
    }
}
