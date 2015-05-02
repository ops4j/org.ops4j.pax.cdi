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
package org.ops4j.pax.cdi.extension.impl.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.enterprise.inject.spi.InjectionPoint;

import org.ops4j.pax.cdi.api.OsgiService;
import org.ops4j.pax.swissbox.tracker.ServiceLookup;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for finding OSGi objects for a given CDI injection point.
 *
 * @author Harald Wellmann
 *
 */
public class InjectionPointOsgiUtils {

    private static Logger log = LoggerFactory.getLogger(InjectionPointOsgiUtils.class);

    /** Hidden constructor of utility class. */
    private InjectionPointOsgiUtils() {
    }

    /**
     * Gets a service reference for the given injection point which must be annotated with
     * {@link OsgiService}.
     *
     * @param ip
     *            injection point
     * @return matching service reference
     */
    @SuppressWarnings("rawtypes")
    public static ServiceReference getServiceReference(InjectionPoint ip) {
        OsgiService os = ip.getAnnotated().getAnnotation(OsgiService.class);
        Type serviceType = ip.getType();
        Class<?> klass = (Class<?>) serviceType;
        String filter = getFilter(klass, os);
        BundleContext bc = getBundleContext(ip);
        return ServiceLookup.getServiceReference(bc, klass.getName(), getTimeout(os), filter);
    }

    private static int getTimeout(OsgiService os) {
        int timeout = os.timeout() == -1 ? 1 : os.timeout();
        return timeout;
    }

    /**
     * Gets a complete LDAP filter string for an OSGi service with the given class and a partial
     * filter.
     *
     * @param serviceType
     *            service type
     * @param qualifier
     *            qualifier with partial filter
     * @return filter string
     */
    public static String getFilter(Class<?> serviceType, OsgiService qualifier) {
        String objectClassClause = "(" + Constants.OBJECTCLASS + "=" + serviceType.getName() + ")";
        String filter = "(&" + objectClassClause + qualifier.filter() + ")";
        log.debug("filter = " + filter);
        return filter;
    }

    /**
     * Gets a complete LDAP filter string for an OSGi service injection point.
     *
     * @param ip
     *            injection point
     * @return filter string
     */
    public static String getFilter(InjectionPoint ip) {
        Class<?> klass = getServiceType(ip);
        OsgiService os = ip.getAnnotated().getAnnotation(OsgiService.class);
        return getFilter(klass, os);
    }

    public static Type getInstanceType(InjectionPoint ip) {
        if (ip.getType() instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) ip.getType();
            Type[] argTypes = parameterizedType.getActualTypeArguments();
            if (argTypes.length > 0) {
                Type instanceType = argTypes[0];
                return instanceType;
            }
        }
        return null;
    }

    public static Class<?> getServiceType(InjectionPoint ip) {
        Type serviceType = getInstanceType(ip);
        if (serviceType == null) {
            serviceType = ip.getType();
        }
        return (Class<?>) serviceType;
    }

    /**
     * Gets the bundle context of the class containing the given injection point.
     *
     * @param ip
     *            injection point
     * @return bundle context
     */
    public static BundleContext getBundleContext(InjectionPoint ip) {
        return getBundleContext(ip.getMember().getDeclaringClass());
    }

    /**
     * Gets the bundle context of the given class.
     *
     * @param ip
     *            injection point
     * @return bundle context
     * @throws IllegalArgumentException
     *             when the class is not from an OSGi bundle
     */
    public static BundleContext getBundleContext(Class<?> klass) {
        BundleContext bc = null;
        try {
            BundleReference bundleRef = BundleReference.class.cast(klass.getClassLoader());
            bc = bundleRef.getBundle().getBundleContext();
        }
        catch (ClassCastException exc) {
            throw new IllegalArgumentException("class " + klass.getName()
                + " is not loaded from an OSGi bundle");
        }
        return bc;
    }
}
