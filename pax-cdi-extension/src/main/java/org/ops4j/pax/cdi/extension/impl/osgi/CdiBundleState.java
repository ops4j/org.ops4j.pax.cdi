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
package org.ops4j.pax.cdi.extension.impl.osgi;

import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.ops4j.pax.cdi.extension.api.runtime.dto.ComponentConfigurationDTO;
import org.ops4j.pax.cdi.extension.api.runtime.dto.ComponentDescriptionDTO;
import org.ops4j.pax.cdi.extension.api.runtime.dto.UnsatisfiedReferenceDTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class CdiBundleState implements BundleStateService {

    public static ServiceRegistration<?> register(BundleContext context) {
        return context.registerService(BundleStateService.class, new CdiBundleState(Registry.getInstance()), null);
    }

    private final Registry registry;

    public CdiBundleState(Registry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "CdiOsgi";
    }

    @Override
    public String getDiag(Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        for (ComponentDescriptionDTO desc : registry.getComponentDescriptionDTOs(bundle)) {
            for (ComponentConfigurationDTO cfg : registry.getComponentConfigurationDTOs(desc)) {
                if (cfg.state != ComponentConfigurationDTO.ACTIVE
                        && cfg.state != ComponentConfigurationDTO.SATISFIED) {
                    sb.append(cfg.description.name).append(" (").append(cfg.id).append(")\n");
                    if ((cfg.state & ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION) != 0) {
                        sb.append("  missing configurations:\n");
                        for (String s : cfg.description.configurationPid) {
                            sb.append("    ").append(s).append("\n");
                        }
                    }
                    if ((cfg.state & ComponentConfigurationDTO.UNSATISFIED_REFERENCE) != 0) {
                        sb.append("  missing references:\n");
                        for (UnsatisfiedReferenceDTO ur : cfg.unsatisfiedReferences) {
                            sb.append("    ").append(ur.name).append("\n");
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    @Override
    public BundleState getState(Bundle bundle) {
        if (bundle.getState() == Bundle.ACTIVE) {
            for (ComponentDescriptionDTO desc : registry.getComponentDescriptionDTOs(bundle)) {
                for (ComponentConfigurationDTO cfg : registry.getComponentConfigurationDTOs(desc)) {
                    if (cfg.state != ComponentConfigurationDTO.ACTIVE
                            && cfg.state != ComponentConfigurationDTO.SATISFIED) {
                        return BundleState.Waiting;
                    }
                }
            }
        }
        return BundleState.Unknown;
    }

}
