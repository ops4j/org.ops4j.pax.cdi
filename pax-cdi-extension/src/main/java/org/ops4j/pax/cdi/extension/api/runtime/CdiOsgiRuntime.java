/*
 * Copyright 2016 Guillaume Nodet
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
package org.ops4j.pax.cdi.extension.api.runtime;

import java.util.Collection;

import org.ops4j.pax.cdi.extension.api.runtime.dto.ComponentConfigurationDTO;
import org.ops4j.pax.cdi.extension.api.runtime.dto.ComponentDescriptionDTO;
import org.osgi.framework.Bundle;

public interface CdiOsgiRuntime {

    /**
     * Returns the component descriptions declared by the specified active
     * bundles.
     *
     * <p>
     * Only component descriptions from active bundles are returned. If the
     * specified bundles have no declared components or are not active, an empty
     * collection is returned.
     *
     * @param bundles The bundles whose declared component descriptions are to
     *        be returned. Specifying no bundles, or the equivalent of an empty
     *        {@code Bundle} array, will return the declared component
     *        descriptions from all active bundles.
     * @return The declared component descriptions of the specified active
     *         {@code bundles}. An empty collection is returned if there are no
     *         component descriptions for the specified active bundles.
     */
    Collection<ComponentDescriptionDTO> getComponentDescriptionDTOs(Bundle... bundles);

    /**
     * Returns the {@link ComponentDescriptionDTO} declared with the specified name
     * by the specified bundle.
     *
     * <p>
     * Only component descriptions from active bundles are returned.
     * {@code null} if no such component is declared by the given {@code bundle}
     * or the bundle is not active.
     *
     * @param bundle The bundle declaring the component description. Must not be
     *        {@code null}.
     * @param name The name of the component description. Must not be
     *        {@code null}.
     * @return The declared component description or {@code null} if the
     *         specified bundle is not active or does not declare a component
     *         description with the specified name.
     */
    ComponentDescriptionDTO getComponentDescriptionDTO(Bundle bundle, String name);

    /**
     * Returns the component configurations for the specified component
     * description.
     *
     * @param description The component description. Must not be {@code null}.
     * @return A collection containing a snapshot of the current component
     *         configurations for the specified component description. An empty
     *         collection is returned if there are none.
     */
    Collection<ComponentConfigurationDTO> getComponentConfigurationDTOs(ComponentDescriptionDTO description);

}
