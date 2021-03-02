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
package org.ops4j.pax.cdi.spi;

import java.util.Collection;

import org.osgi.framework.Bundle;

/**
 * Context class loader of {@link org.ops4j.pax.cdi.spi.CdiContainer} sees all the required
 * bundles - from provider, extension(s), etc.
 * In Web environment (pax-web) we already have necessary class loader configured, we may
 * have to <em>merge</em> delegated bundles to provide necessary bundle closure.
 */
public interface CdiClassLoaderBuilder {

    /**
     * Create (or obtain and modify) a {@link ClassLoader} to be set as {@link org.ops4j.pax.cdi.spi.CdiContainer}'s
     * <em>context class loader</em>.
     *
     * @param environment meaningless in JavaSE env, but it may be a reference to {@code javax.servlet.ServletContext}
     * in web environment
     * @param beanArchive {@link Bundle} for the bean archive for which the container is being created
     * @param extensionBundles additional {@link Bundle bundles} for all wired extensions
     * @param additionalBundles additional {@link Bundle bundles} - e.g. the CDI provider itself
     * @return
     */
    ClassLoader buildContextClassLoader(Object environment,
                                        Bundle beanArchive,
                                        Collection<Bundle> extensionBundles,
                                        Collection<Bundle> additionalBundles);

}
