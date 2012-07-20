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
package org.ops4j.pax.cdi.weld.impl.bda;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.resources.spi.ResourceLoadingException;
import org.ops4j.pax.cdi.weld.impl.EnumerationList;
import org.osgi.framework.Bundle;

public class BundleResourceLoader implements ResourceLoader {
    private Bundle bundle;

    public BundleResourceLoader(Bundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public Class<?> classForName(String name) {
        try {
            Class<?> clazz = bundle.loadClass(name);
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new ResourceLoadingException(e);
        }
    }

    @Override
    public URL getResource(String name) {
        return bundle.getResource(name);
    }

    @Override
    public Collection<URL> getResources(String name) {
        try {
            return new EnumerationList<URL>(bundle.getResources(name));
        } catch (IOException e) {
            throw new ResourceLoadingException(e);
        }
    }

    @Override
    public void cleanup() {
        // empty
    }
}
