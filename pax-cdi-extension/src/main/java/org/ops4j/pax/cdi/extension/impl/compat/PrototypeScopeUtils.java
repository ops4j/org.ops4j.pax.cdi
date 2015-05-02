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
package org.ops4j.pax.cdi.extension.impl.compat;

import org.ops4j.pax.cdi.spi.util.Exceptions;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for dealing with prototype scope in OSGi 6.0 or higher.
 *
 * @author Harald Wellmann
 *
 */
public class PrototypeScopeUtils {

    private static Logger log = LoggerFactory.getLogger(PrototypeScopeUtils.class);

    private static final Version OSGI6_FRAMEWORK_VERSION = new Version(1, 8, 0);

    private static volatile Class<?> wrapperClass;

    private PrototypeScopeUtils() {
        // Hidden utility class constructor
    }

    /**
     * Creates a {@link ServiceObjectsWrapper} for the given bundle context and service reference.
     *
     * @param bc
     *            bundle context
     * @param serviceReference
     *            service reference
     * @return wrapper object, with type depending on the OSGi framework version
     */
    @SuppressWarnings("unchecked")
    public static <S> ServiceObjectsWrapper<S> createServiceObjectsWrapper(BundleContext bc,
        ServiceReference<S> serviceReference) {
        ServiceObjectsWrapper<S> w = (ServiceObjectsWrapper<S>) instantiateWrapper(bc);
        w.init(bc, serviceReference);
        return w;
    }

    @SuppressWarnings("unchecked")
    private static <S> ServiceObjectsWrapper<S> instantiateWrapper(BundleContext bc) {
        try {
            Object instance = getWrapperClass(bc).newInstance();
            return (ServiceObjectsWrapper<S>) instance;
        }
        catch (InstantiationException | IllegalAccessException exc) {
            log.error("", exc);
        }
        return null;
    }

    private static Class<?> getWrapperClass(BundleContext bc) {
        if (wrapperClass == null) {
            wrapperClass = loadWrapperClass(bc);
        }
        return wrapperClass;
    }

    private static Class<?> loadWrapperClass(BundleContext bc) {
        String simpleClassName = getWrapperClassName(bc);
        String className = String.format("%s.%s", PrototypeScopeUtils.class.getPackage().getName(),
            simpleClassName);
        try {
            return Class.forName(className, true, PrototypeScopeUtils.class.getClassLoader());
        }
        catch (ClassNotFoundException exc) {
            throw Exceptions.unchecked(exc);
        }
    }

    private static String getWrapperClassName(BundleContext bc) {
        if (hasPrototypeScope(bc)) {
            return "Osgi6ServiceObjectsWrapper";
        }
        else {
            return "Osgi5ServiceObjectsWrapper";
        }
    }

    /**
     * Checks if the current OSGi framework supports prototype scope.
     *
     * @param bc
     *            bundle context of any bundle
     * @return true if the framework version is 1.8 or higher
     */
    public static boolean hasPrototypeScope(BundleContext bc) {
        Version actualVersion = Version.parseVersion(bc.getProperty(Constants.FRAMEWORK_VERSION));
        return actualVersion.compareTo(OSGI6_FRAMEWORK_VERSION) >= 0;
    }
}
