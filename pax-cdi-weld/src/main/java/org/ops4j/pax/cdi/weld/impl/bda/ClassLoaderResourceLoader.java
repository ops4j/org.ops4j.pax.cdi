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
 *
 * Copied from Weld, where this class is not exported.
 */
package org.ops4j.pax.cdi.weld.impl.bda;



/**
 * A (@link ResourceLoader} implementation that uses a specific @{link ClassLoader}.
 * <p>
 * Not exported by Weld, so we use a local copy for Pax CDI.
 *
 * @author Marius Bogoevici
 *
 */
public class ClassLoaderResourceLoader extends AbstractClassLoaderResourceLoader {
    private ClassLoader cl;

    /**
     * Creates a resource loader for the given class loader.
     * @param classLoader
     */
    public ClassLoaderResourceLoader(ClassLoader classLoader) {
        this.cl = classLoader;
    }

    @Override
    protected ClassLoader classLoader() {
        return cl;
    }

    @Override
    public void cleanup() {
        this.cl = null;
    }
}
