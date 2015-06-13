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

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.weld.serialization.spi.ProxyServices;
import org.ops4j.pax.cdi.spi.util.Exceptions;

/**
 * Implements {@link ProxyServices} based on the extended bundle class loader which must
 * be set as thread context class loader.
 *
 * @author Harald Wellmann
 *
 */
public class OsgiProxyService implements ProxyServices {

    private ClassLoader loader = Thread.currentThread().getContextClassLoader();

    @Override
    public ClassLoader getClassLoader(Class<?> proxiedBeanType) {
        return proxiedBeanType.getClassLoader();
    }

    @Override
    public Class<?> loadBeanClass(final String className) {
        try {
            return (Class<?>) AccessController.doPrivileged(new LoadClass(className));
        }
        catch (PrivilegedActionException pae) {
            throw Exceptions.unchecked(pae);
        }
    }

    @Override
    public void cleanup() {
        // empty
    }

    private class LoadClass implements PrivilegedExceptionAction<Object> {

        private String className;

        private LoadClass(String className) {
            this.className = className;
        }

        @Override
        public Object run() throws ClassNotFoundException  {
            return Class.forName(className, true, loader);
        }
    }
}
