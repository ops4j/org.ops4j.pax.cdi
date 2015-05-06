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

import java.util.Collection;
import java.util.Collections;

import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;

/**
 * Implements {@link BeanDeploymentArchive} for bundle deployments.
 *
 * @author Harald Wellmann
 *
 */
public class BundleBeanDeploymentArchive implements BeanDeploymentArchive {

    private String id;

    private Collection<String> beanClasses;

    private BeansXml beansXml;

    private ServiceRegistry serviceRegistry;

    /**
     * Creates a bean deployment archive with the given ID. The ID is suffixed with the bundle ID.
     *
     * @param id
     *            archive identity
     */
    public BundleBeanDeploymentArchive(String id) {
        this.id = id;
        this.serviceRegistry = new SimpleServiceRegistry();
    }

    @Override
    public Collection<String> getBeanClasses() {
        return beanClasses;
    }

    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return Collections.emptyList();
    }

    @Override
    public BeansXml getBeansXml() {
        return beansXml;
    }

    @Override
    public Collection<EjbDescriptor<?>> getEjbs() {
        return Collections.emptyList();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ServiceRegistry getServices() {
        return serviceRegistry;
    }

    public void setBeanClasses(Collection<String> classes) {
        this.beanClasses = Collections.unmodifiableCollection(classes);
    }

    public void setBeansXml(BeansXml beansXml) {
        this.beansXml = beansXml;
    }
}
