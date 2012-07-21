package org.ops4j.pax.cdi.extension.impl;

import java.lang.reflect.Type;

import javax.enterprise.inject.spi.InjectionPoint;

import org.ops4j.pax.cdi.api.OsgiService;
import org.ops4j.pax.swissbox.framework.ServiceLookup;
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

    public static boolean isServiceAvailable(InjectionPoint ip) {
        return InjectionPointOsgiUtils.getServiceReference(ip) != null;
    }

    @SuppressWarnings("rawtypes")
    public static ServiceReference getServiceReference(InjectionPoint ip) {
        OsgiService qualifier = ip.getAnnotated().getAnnotation(OsgiService.class);
        Type serviceType = ip.getType();
        Class<?> klass = (Class<?>) serviceType;
        String filter = getFilter(klass, qualifier);
        BundleContext bc = getBundleContext(ip);
        return ServiceLookup.getServiceReference(bc, klass.getName(), qualifier.timeout(), filter);
    }

    public static Object lookupService(InjectionPoint ip) {
        Class<?> klass = (Class<?>) ip.getType();
        OsgiService os = ip.getAnnotated().getAnnotation(OsgiService.class);

        BundleContext bc = getBundleContext(ip);

        String filter = getFilter(klass, os);
        int timeout = os.timeout() == -1 ? 1 : os.timeout();
        Object service = ServiceLookup.getService(bc, klass, timeout, filter);
        return service;
    }

    public static String getFilter(Class<?> serviceType, OsgiService qualifier) {
        String objectClassClause = "(" + Constants.OBJECTCLASS + "=" + serviceType.getName() + ")";
        String filter = "(&" + objectClassClause + qualifier.filter() + ")";
        log.debug("filter = " + filter);
        return filter;
    }

    public static BundleContext getBundleContext(InjectionPoint ip) {
        return getBundleContext(ip.getMember().getDeclaringClass());
    }

    public static BundleContext getBundleContext(Class<?> klass) {
        BundleContext bc = null;
        try {
            BundleReference bundleRef = BundleReference.class.cast(klass.getClassLoader());
            bc = bundleRef.getBundle().getBundleContext();
        }
        catch (ClassCastException exc) {
            log.error("class " + klass.getName() + " is not loaded from an OSGi bundle");
            throw exc;
        }
        return bc;
    }
}
