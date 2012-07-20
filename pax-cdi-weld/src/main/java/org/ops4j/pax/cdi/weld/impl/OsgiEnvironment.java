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
package org.ops4j.pax.cdi.weld.impl;

import org.jboss.weld.bootstrap.api.Environment;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.resources.spi.ScheduledExecutorServiceFactory;

import java.util.HashSet;
import java.util.Set;

public class OsgiEnvironment implements Environment {
    
    private static OsgiEnvironment instance;
    
    private Set<Class<? extends Service>> deploymentServices = new HashSet<Class<? extends Service>>(); 
    private Set<Class<? extends Service>> bdaServices = new HashSet<Class<? extends Service>>(); 

    
    private OsgiEnvironment() {
        deploymentServices.add(ScheduledExecutorServiceFactory.class);
        bdaServices.add(ResourceLoader.class);
    }
    
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