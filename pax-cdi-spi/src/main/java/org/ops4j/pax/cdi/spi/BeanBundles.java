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

package org.ops4j.pax.cdi.spi;

import static org.ops4j.pax.cdi.api.Constants.CDI_EXTENDER;
import static org.ops4j.pax.cdi.api.Constants.EXTENDER_CAPABILITY;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;


/**
 * @author Harald Wellmann
 *
 */
public class BeanBundles {
    
    private BeanBundles() {
        
    }

    public static boolean isBeanBundle(Bundle candidate) {
        List<BundleWire> wires = candidate.adapt(BundleWiring.class).getRequiredWires(
            EXTENDER_CAPABILITY);
        for (BundleWire wire : wires) {
            Object object = wire.getCapability().getAttributes().get(EXTENDER_CAPABILITY);
            if (object instanceof String) {
                String extender = (String) object;
                if (extender.equals(CDI_EXTENDER)) {
                    return true;
                }
            }
        }
        return false;
    }
}
