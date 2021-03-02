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
package org.ops4j.pax.cdi.weld.impl;

import java.util.HashSet;
import java.util.Set;

import org.jboss.weld.bootstrap.api.Environment;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.resources.spi.ResourceLoader;

/**
 * Environment for OSGi bean bundles.
 *
 * @author Harald Wellmann
 *
 */
public class OsgiEnvironment implements Environment {

    private static OsgiEnvironment instance;

    private final Set<Class<? extends Service>> deploymentServices = new HashSet<>();

    private final Set<Class<? extends Service>> bdaServices = new HashSet<>();

    private OsgiEnvironment() {
        bdaServices.add(ResourceLoader.class);
    }

    /**
     * Get singleton instance of this environment.
     *
     * @return singleton environment
     */
    public static synchronized OsgiEnvironment getInstance() {
        if (instance == null) {
            instance = new OsgiEnvironment();
        }
        return instance;
    }

    @Override
    public Set<Class<? extends Service>> getRequiredDeploymentServices() {
        return deploymentServices;
    }

    @Override
    public Set<Class<? extends Service>> getRequiredBeanDeploymentArchiveServices() {
        return bdaServices;
    }
}
