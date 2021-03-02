/*
 * Copyright 2021 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.spi.scan;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Model;
import javax.enterprise.inject.Stereotype;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.AnnotationFinder.AnnotationInfo;
import org.apache.xbean.finder.AnnotationFinder.ClassInfo;
import org.ops4j.pax.cdi.api.BundleScoped;
import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.PrototypeScoped;
import org.ops4j.pax.cdi.api.Service;
import org.ops4j.pax.cdi.api.SingletonScoped;
import org.osgi.framework.Bundle;

/**
 * Scans a bundle for candidate managed bean classes. The scanner only looks at bundle entries but
 * does not load classes. The set of candidate classes is passed to the CDI implementation which
 * will discard some of the candidates, e.g. if the class cannot be loaded or does not have a
 * default constructor.
 * <p>
 * The scanner returns all classes contained in the given bundle, including embedded archives and
 * directories from the bundle classpath, and all classes visible from required bundle wires
 * (package imports or required bundles), provided that the exporting bundle is a bean bundle.
 *
 * @author Harald Wellmann
 *
 */
public class BeanScanner {

    private static Set<String> beanDefiningAnnotations;

    private BeanBundleFilter filter;
    private BundleArchive archive;
    private Set<String> beanClasses = new HashSet<>();
    private BeanAnnotationFinder finder;

    static {
        beanDefiningAnnotations = new HashSet<String>();
        for (Class<?> klass : Arrays.asList(Dependent.class, RequestScoped.class,
            ConversationScoped.class, SessionScoped.class, ApplicationScoped.class,
            javax.interceptor.Interceptor.class, javax.decorator.Decorator.class, Model.class,
            NormalScope.class, Stereotype.class, BundleScoped.class, PrototypeScoped.class,
            SingletonScoped.class,
            Component.class, Service.class)) {
            beanDefiningAnnotations.add(klass.getName());
        }
    }

    public BeanScanner(Bundle bundle, BeanDescriptorParser parser) {
        this.filter = new BeanBundleFilter(parser);
        this.archive = new BundleArchive(bundle, filter);
    }

    /**
     * Returns the class names of all bean candidate classes.
     *
     * @return unmodifiable set
     */
    public Set<String> getBeanClasses() {
        return Collections.unmodifiableSet(beanClasses);
    }

    /**
     * Return the URLs of all bean descriptors (beans.xml).
     *
     * @return unmodifiable set
     */
    public Set<URL> getBeanDescriptors() {
        Set<URL> urls = new HashSet<>(filter.getBeanDescriptors());
        return Collections.unmodifiableSet(urls);
    }

    /**
     * Scans the given bundle and all imports for bean classes.
     */
    public void scan() {
        finder = new BeanAnnotationFinder(archive);
        for (String className : finder.getAnnotatedClassNames()) {
            if (isBeanClass(className)) {
                beanClasses.add(className);
            }
        }
    }

    private boolean isBeanClass(String className) {
        Bundle provider = archive.getProvider(className);
        BeanDescriptor descriptor = filter.findDescriptor(provider);
        if (descriptor.getBeanDiscoveryMode().equals(BeanDiscoveryMode.ANNOTATED)) {
            ClassInfo classInfo = finder.getClassInfo(className);
            return isBeanAnnotatedClass(classInfo);
        }
        return true;
    }

    protected boolean isBeanAnnotatedClass(ClassInfo classInfo) {
        for (AnnotationFinder.AnnotationInfo annotationInfo : classInfo.getAnnotations()) {
            if (isBeanAnnotation(annotationInfo)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBeanAnnotation(AnnotationInfo annotationInfo) {
        return beanDefiningAnnotations.contains(annotationInfo.getName());
    }
}
