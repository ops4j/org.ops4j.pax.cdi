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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;


/**
 * @author Harald Wellmann
 *
 */
public class BeanBundles {
    
    private static Map<ClassLoader, Bundle> bundleMap = new HashMap<ClassLoader, Bundle>();
    private static Set<Bundle> beanBundles = new HashSet<Bundle>();
    
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
    
    public static boolean isActiveBeanBundle(Bundle candidate) {
        return beanBundles.contains(candidate);
    }
    
    public static synchronized void addBundle(ClassLoader cl, Bundle bundle) {
        bundleMap.put(cl, bundle);
        beanBundles.add(bundle);
    }

    public static synchronized void removeBundle(ClassLoader cl, Bundle bundle) {
        bundleMap.remove(cl);
        beanBundles.remove(bundle);
    }
    
    public static synchronized Bundle getBundle(ClassLoader cl) {
        return bundleMap.get(cl);
    }

    public static synchronized Bundle getCurrentBundle() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return bundleMap.get(cl);
    }
}
