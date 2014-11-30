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
package org.ops4j.pax.cdi.openwebbeans.impl;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.ops4j.pax.cdi.api.Constants;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A weaving hook which adds a dynamic package import for managed bean proxies for all
 * classes from bean bundles.
 *
 * @author Harald Wellmann
 */
public class ProxyWeavingHook implements WeavingHook {

    private static Logger log = LoggerFactory.getLogger(ProxyWeavingHook.class);

    private Map<Bundle, Boolean> bundleMap = new WeakHashMap<Bundle, Boolean>();


    @Override
    public void weave(WovenClass wovenClass) {
        Bundle bundle = wovenClass.getBundleWiring().getBundle();
        Boolean seen = bundleMap.get(bundle);
        if (seen != null) {
            return;
        }
        boolean requiresWeaving = false;
        if (isBeanBundle(bundle) || isExtension(bundle)) {
            log.debug("weaving {}", wovenClass.getClassName());
            wovenClass.getDynamicImports().add("org.apache.webbeans.proxy");
            wovenClass.getDynamicImports().add("org.apache.webbeans.intercept");
            requiresWeaving = true;
        }
        bundleMap.put(bundle, requiresWeaving);
    }

    /**
     * TODO Copied from BeanBundles.isBeanBundle(). Using that method from pax-cdi-spi
     * causes a ClassCircularityError. Is there a better way to avoid this?
     * @param candidate
     * @return
     */
    private static boolean isBeanBundle(Bundle candidate) {
        List<BundleWire> wires = candidate.adapt(BundleWiring.class).getRequiredWires(
            Constants.EXTENDER_CAPABILITY);
        for (BundleWire wire : wires) {
            Object object = wire.getCapability().getAttributes().get(Constants.EXTENDER_CAPABILITY);
            if (object instanceof String) {
                String extender = (String) object;
                if (extender.equals(Constants.CDI_EXTENDER)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isExtension(Bundle candidate) {
        if (candidate.getSymbolicName().equals(Constants.CDI_EXTENSION_CAPABILITY)) {
            return true;
        }
        List<BundleWire> wires = candidate.adapt(BundleWiring.class).getRequiredWires(
            Constants.CDI_EXTENSION_CAPABILITY);
        return !wires.isEmpty();
    }
}
