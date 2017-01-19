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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import static org.ops4j.pax.cdi.spi.Constants.*;

/**
 * Registry for bean bundles.
 *
 * @author Harald Wellmann
 *
 */
public class BeanBundles {

    private static Map<ClassLoader, Bundle> bundleMap = new HashMap<>();
    private static Set<Bundle> bundleSet = new HashSet<>();

    private BeanBundles() {
        // hidden utility class constructor
    }

    /**
     * Checks if the given bundle is a bean bundle by inspecting its wiring. The bundle is a bean
     * bundle if it is wired to the {@ode Constants#CDI_EXTENDER} capability.
     *
     * @param candidate
     *            candidate bundle
     * @return true if bundle is a bean bundle
     */
    public static boolean isBeanBundle(Bundle candidate) {
        BundleWiring wiring = candidate.adapt(BundleWiring.class);
        if (wiring == null) {
            return false;
        }
        List<BundleWire> wires = wiring.getRequiredWires(EXTENDER_CAPABILITY);
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

    /**
     * Checks is the bundle is an active bean bundle.
     *
     * @param candidate
     *            candidate bean bundle
     * @return true if the bundle is a bean bundle with status ACTIVE and a started CDI container
     */
    public static boolean isActiveBeanBundle(Bundle candidate) {
        return bundleSet.contains(candidate);
    }

    /**
     * Adds a bean bundle.
     *
     * @param cl
     *            extended bundle class loader
     * @param bundle
     *            bean bundle
     */
    public static synchronized void addBundle(ClassLoader cl, Bundle bundle) {
        bundleMap.put(cl, bundle);
        bundleSet.add(bundle);
    }

    /**
     * Removes a bean bundle.
     *
     * @param cl
     *            extended bundle class loader
     * @param bundle
     *            bean bundle
     */
    public static synchronized void removeBundle(ClassLoader cl, Bundle bundle) {
        bundleMap.remove(cl);
        bundleSet.remove(bundle);
    }

    /**
     * Gets the bean bundle for the given extended bundle class loader.
     *
     * @param cl
     *            class loader
     * @return bean bundle, or null
     */
    public static synchronized Bundle getBundle(ClassLoader cl) {
        return bundleMap.get(cl);
    }

    /**
     * Gets the bean bundle correspsonding to the current thread context class loader.
     *
     * @param cl
     *            class loader
     * @return bean bundle associated to TCCL, or null
     */
    public static synchronized Bundle getCurrentBundle() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return bundleMap.get(cl);
    }

    /**
     * Finds the CDI extension bundles wired to this given bundle. This method recursively calls
     * itself to track examine wirings of wired bundles.
     *
     * @param bundle
     *            bean bundle
     * @param extensions
     *            set of found extension bundles.
     */
    public static void findExtensions(Bundle bundle, Set<Bundle> extensions) {
        List<BundleWire> wires = bundle.adapt(BundleWiring.class).getRequiredWires(null);
        if (wires != null) {
            for (BundleWire wire : wires) {
                String ns = wire.getCapability().getNamespace();
                if (CDI_EXTENSION_CAPABILITY.equals(ns)
                    || PAX_CDI_EXTENSION_CAPABILITY.equals(ns)) {
                    Bundle b = wire.getProviderWiring().getBundle();
                    extensions.add(b);
                    findExtensions(b, extensions);
                }
            }
        }
    }
}
