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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.webbeans.spi.BDABeansXmlScanner;
import org.apache.webbeans.spi.ScannerService;
import org.apache.xbean.osgi.bundle.util.BundleClassLoader;
import org.apache.xbean.osgi.bundle.util.BundleUtils;
import org.apache.xbean.osgi.bundle.util.DelegatingBundle;
import org.ops4j.pax.cdi.spi.scan.BeanScanner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BundleScannerService implements ScannerService {
    
    private Logger log = LoggerFactory.getLogger(BundleScannerService.class);

    private BeanScanner scanner;
    private Bundle bundle;
    private Set<Class<?>> beanClasses;
    private Map<String, Set<String>> classAnnotations;
    
    
    
    public BundleScannerService() {
        classAnnotations = new HashMap<String, Set<String>>();
    }

    @Override
    public void init(Object object) {
        // empty
    }

    @Override
    public void scan() {        
        bundle = BundleUtils.getContextBundle(true);
        scanner = new BeanScanner(bundle);
        scanner.scan();
    }

    @Override
    public void release() {
        scanner = null;
        bundle = null;
        beanClasses = null;
        classAnnotations.clear();
    }

    @Override
    public Set<URL> getBeanXmls() {
        return scanner.getBeanDescriptors();
    }

    @Override
    public Set<Class<?>> getBeanClasses() {
        if (beanClasses == null) {
            beanClasses = new HashSet<Class<?>>();
            for (String className : scanner.getBeanClasses()) {
                try {
                    Class<?> klass = bundle.loadClass(className);
                    beanClasses.add(klass);
                }
                catch (ClassNotFoundException exc) {
                    log.debug("cannot load {}", className);
                }
            }
        }
        return beanClasses;
    }

    @Override
    public Set<String> getAllAnnotations(String className) {
        Set<String> annotations = classAnnotations.get(className);
        if (annotations == null) {
            annotations = new HashSet<String>();
            try {
                Class<?> klass = bundle.loadClass(className);
                collectAnnotations(annotations, klass);
            }
            catch (ClassNotFoundException exc) {
                log.debug("cannot load class {}", className);
            }
            classAnnotations.put(className, annotations);
        }
        return annotations;
    }

    private Set<String> collectAnnotations(Set<String> annotations, Class<?> cls) {

        addAnnotations(annotations, cls.getAnnotations());

        Constructor<?>[] constructors = cls.getDeclaredConstructors();
        for (Constructor<?> c : constructors) {
            addAnnotations(annotations, c.getAnnotations());
        }

        Field[] fields = cls.getDeclaredFields();
        for (Field f : fields) {
            addAnnotations(annotations, f.getAnnotations());
        }

        Method[] methods = cls.getDeclaredMethods();
        for (Method m : methods) {
            addAnnotations(annotations, m.getAnnotations());

            Annotation[][] paramsAnns = m.getParameterAnnotations();
            for (Annotation[] pAnns : paramsAnns) {
                addAnnotations(annotations, pAnns);
            }
        }

        return annotations;
    }

    private void addAnnotations(Set<String> annStrings, Annotation[] annotations) {
        for (Annotation ann : annotations) {
            annStrings.add(ann.getClass().getSimpleName());
        }
    }
    
    @Override
    public boolean isBDABeansXmlScanningEnabled() {
        return false;
    }

    @Override
    public BDABeansXmlScanner getBDABeansXmlScanner() {
        return null;
    }
    
    /**
     * Returns bundle (if any) associated with current thread's context classloader.
     *
     * @param unwrap if true and if the bundle associated with the context classloader is a
     *        {@link DelegatingBundle}, this function will return the main application bundle
     *        backing with the {@link DelegatingBundle}. Otherwise, the bundle associated with
     *        the context classloader is returned as is. See {@link BundleClassLoader#getBundle(boolean)}
     *        for more information.
     * @return The bundle associated with the current thread's context classloader. Might be null.
     */
    public static Bundle getContextBundle(boolean unwrap) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader instanceof BundleClassLoader) {
            return ((BundleClassLoader) classLoader).getBundle(unwrap);
        } else if (classLoader instanceof BundleReference) {
            return ((BundleReference) classLoader).getBundle();
        } else {
            return null;
        }
    }
}
