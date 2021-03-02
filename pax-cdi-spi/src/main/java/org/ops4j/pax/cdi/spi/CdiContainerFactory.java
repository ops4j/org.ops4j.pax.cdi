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
package org.ops4j.pax.cdi.spi;

import java.util.Collection;

import org.osgi.framework.Bundle;

/**
 * A {@code CdiContainerFactory} creates a CDI container for OSGi bundles using a composite class
 * loader including the given bundle, the CDI implementation bundle and any CDI extension bundles
 * resolved at the given moment in time.
 * <p>
 * At most one {@code CdiContainerFactory} implementation should be available in each OSGi system.
 * Implementations of this interface shall adapt a given CDI implementation to the OSGi environment.
 * <p>
 * A {@code CdiContainerFactory} is able to create multiple {@link CdiContainer}s in parallel, one
 * for each CDI-enabled bundle, also called <em>bean bundle</em> for short.
 *
 * @author Harald Wellmann
 *
 */
public interface CdiContainerFactory {

    /**
     * Creates a CDI container for the given bundle. The bundle is assumed to be started. The CDI
     * container has a lifecycle of its own. The container returned by this method is not yet
     * started.
     *
     * @param bundle
     *            a bundle to be extended with a CDI container
     * @param extensions
     *            the collection of extension bundles
     * @return
     */
    CdiContainer createContainer(Bundle bundle, Collection<Bundle> extensions);

    /**
     * Gets the CDI container for the given bundle, or null if the bundle is not a bean bundle.
     *
     * @param bundle
     *            bundle
     * @return associated CDI container
     */
    CdiContainer getContainer(Bundle bundle);

    /**
     * Gets the collection of all active CDI containers created by this factory.
     *
     * @return
     */
    Collection<CdiContainer> getContainers();

    /**
     * Removes the CDI container for the given bundle. This method must be called after the bundle
     * is stopped.
     *
     * @param bundle
     */
    void removeContainer(Bundle bundle);

    /**
     * Returns a name identifying the CDI provider. This should be a CDI container implementation
     * class name.
     *
     * @return CDI provider name
     */
    String getProviderName();

}
