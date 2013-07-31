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

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.InjectionPoint;

import org.ops4j.pax.cdi.api.Timeout;
import org.ops4j.pax.swissbox.tracker.ServiceLookup;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.Filter;
import org.osgi.service.cdi.Service;
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

    public static boolean isServiceAvailable(InjectionPoint ip) {
        return InjectionPointOsgiUtils.getServiceReference(ip) != null;
    }

    @SuppressWarnings("rawtypes")
    public static ServiceReference getServiceReference(InjectionPoint ip) {
        Service os = ip.getAnnotated().getAnnotation(Service.class);
        Timeout timeout = ip.getAnnotated().getAnnotation(Timeout.class);
        Type serviceType = ip.getType();
        Class<?> klass = (Class<?>) serviceType;
        String filter = getFilter(ip);
        BundleContext bc = getBundleContext(ip);
        return ServiceLookup.getServiceReference(bc, klass.getName(), timeout != null ? timeout.value() : 0, filter);
    }

    public static Object lookupService(InjectionPoint ip) {
        Class<?> klass = (Class<?>) ip.getType();
        Service os = ip.getAnnotated().getAnnotation(Service.class);
        Timeout timeout = ip.getAnnotated().getAnnotation(Timeout.class);

        BundleContext bc = getBundleContext(ip);

        String filter = getFilter(ip);
        Object service = ServiceLookup.getService(bc, klass, timeout != null ? timeout.value() : 0, filter);
        return service;
    }

    public static String getFilter(InjectionPoint ip) {
        List<String> filters = new ArrayList<String>();
        filters.add("(" + Constants.OBJECTCLASS + "=" + getServiceType(ip).getName() + ")");
        for (Annotation annotation : ip.getAnnotated().getAnnotations()) {
            if (annotation.annotationType() == Filter.class) {
                String str = ((Filter) annotation).value();
                if (!str.startsWith("(") || !str.endsWith(")")) {
                    str = "(" + str + ")";
                }
                filters.add(str);
            } else {
                Class<? extends Annotation> clazz = annotation.annotationType();
                Filter flt = clazz.getAnnotation(Filter.class);
                if (flt != null) {
                    try {
                        String key = (flt.value() == null || flt.value().isEmpty()) ? clazz.getSimpleName() : flt.value();
                        Object val = clazz.getMethod("value").invoke(annotation);
                        String str = "(" + key + "=" + (val == null ? "null" : val.toString()) + ")";
                        filters.add(str);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        if (filters.size() == 0) {
            return "";
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("(&");
            for (String str : filters) {
                sb.append(str);
            }
            sb.append(")");
            return sb.toString();
        }
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
