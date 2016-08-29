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

import java.util.List;

import org.apache.webbeans.spi.LoaderService;

/**
 * Implements {@link LoaderService} for OSGi bundles.
 *
 * @author Harald Wellmann
 *
 */
public class BundleLoaderService implements LoaderService {

    @Override
    public <T> List<T> load(Class<T> serviceType) {
        return load(serviceType, Thread.currentThread().getContextClassLoader());
    }

    @Override
    public <T> List<T> load(Class<T> serviceType, ClassLoader classLoader) {
        SafeServiceLoader serviceLoader = new SafeServiceLoader(classLoader);
        return serviceLoader.load(serviceType.getName());
    }
}
