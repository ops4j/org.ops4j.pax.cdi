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
package org.ops4j.pax.cdi.weld.impl.util;

import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.weld.interceptor.proxy.LifecycleMixin;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.jboss.weld.util.Types;
import org.ops4j.pax.cdi.spi.util.Exceptions;
import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

/**
 * Implements {@link ProxyServices} for Weld in OSGi runtime.
 *
 * @see org.ops4j.pax.cdi.weld.impl.ProxyWeavingHook
 *
 * @author Harald Wellmann
 *
 */
public class OsgiProxyService implements ProxyServices {

    private final BeanManager manager;

    private final DelegatingBundle delegate;

    private final ClassLoader loader;

    public OsgiProxyService(BeanManager manager, ClassLoader parent) {
        this.manager = manager;
        // Initiate the delegate classloader to contain the Weld mixin classes
        // TODO: use version from xbean-bundleutils when it provides a way to avoid
        // adding the same bundle multiple times
        this.delegate = new DelegatingBundle(FrameworkUtil.getBundle(LifecycleMixin.class));
        this.loader = new BundleClassLoader(delegate, parent);
    }

    @Override
    public ClassLoader getClassLoader(Class<?> proxiedBeanType) {
        if (!Modifier.isPublic(proxiedBeanType.getModifiers()) &&
            !Modifier.isProtected(proxiedBeanType.getModifiers())) {
            // For package-private bean type, we must used the same defining classloader
            // as that of the bean type otherwise IllegalAccessError is thrown and there
            // seems to be no obvious way to do proper adaptation of the classloader
            // in case the bean type closure spans multiple bundles.
            // For Weld mixin classes, we rely on the ProxyWeavingHook that adds dynamic
            // imports for the corresponding packages.
            return proxiedBeanType.getClassLoader();
        }
        else if (Extension.class.isAssignableFrom(proxiedBeanType)) {
            // It happens that extensions often declare package-private or public methods
            // whose return type is package-private hence forcing to define the corresponding
            // proxy classes in the same defining classloaders as those of the extension types
            // otherwise IllegalAccessError is thrown.
            return proxiedBeanType.getClassLoader();
        }
        // TODO: it may be necessary to iterate over the whole list of proxied methods from
        // the type closure and check for package-private or public methods whose parameters
        // or return type are package-private.
        else {
            // For non package-private bean type, we retrieve its type closure that may
            // span multiple bundles and add then to list of the delegating bundles.
            // We use the bean managed to leverage its annotated types cache.
            for (Class<?> type : Types.getRawTypes(manager.createAnnotatedType(proxiedBeanType).getTypeClosure())) {
                // TODO: do not add if already there
                Bundle bundle = FrameworkUtil.getBundle(type);
                if (bundle != null) {
                    delegate.addBundle(bundle);
                }
            }
            return loader;
        }
    }

    @Override
    public Class<?> loadBeanClass(final String className) {
        try {
            return AccessController.doPrivileged(new LoadClass(className));
        }
        catch (PrivilegedActionException pae) {
            throw Exceptions.unchecked(pae);
        }
    }

    @Override
    public void cleanup() {
        // empty
    }

    private class LoadClass implements PrivilegedExceptionAction<Class<?>> {

        private final String className;

        private LoadClass(String className) {
            this.className = className;
        }

        @Override
        public Class<?> run() throws ClassNotFoundException {
            return Class.forName(className, true, loader);
        }
    }
}
