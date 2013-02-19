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

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.ops4j.pax.cdi.spi.scan.BeanScanner;
import org.ops4j.pax.cdi.weld.impl.OsgiProxyService;
import org.osgi.framework.Bundle;

public class BundleDeployment implements Deployment {

    private ServiceRegistry serviceRegistry;

    private Iterable<Metadata<Extension>> extensions;
    private BundleBeanDeploymentArchive beanDeploymentArchive;

    public BundleDeployment(Bundle bundle, Collection<URL> descriptors, Bootstrap bootstrap) {

        serviceRegistry = new SimpleServiceRegistry();
        serviceRegistry.add(ProxyServices.class, new OsgiProxyService());
        extensions = bootstrap.loadExtensions(Thread.currentThread().getContextClassLoader());

        createBeanDeploymentArchive(bundle, descriptors, bootstrap);
    }

    private void createBeanDeploymentArchive(Bundle bundle, Collection<URL> descriptors, Bootstrap bootstrap) {
        BeanScanner scanner = new BeanScanner(bundle);
        scanner.scan();
        beanDeploymentArchive = new BundleBeanDeploymentArchive("pax-cdi-bda"
            + bundle.getBundleId());
        beanDeploymentArchive.setBeansXml(bootstrap.parse(descriptors));
        beanDeploymentArchive.setBeanClasses(scanner.getBeanClasses());
        ResourceLoader loader = new BundleResourceLoader(bundle);
        beanDeploymentArchive.getServices().add(ResourceLoader.class, loader);
    }

    @Override
    public List<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return Collections.<BeanDeploymentArchive> singletonList(beanDeploymentArchive);
    }

    @Override
    public BeanDeploymentArchive loadBeanDeploymentArchive(Class<?> beanClass) {
        return beanDeploymentArchive;
    }

    public BeanDeploymentArchive getBeanDeploymentArchive() {
        return beanDeploymentArchive;
    }

    @Override
    public ServiceRegistry getServices() {
        return serviceRegistry;
    }

    @Override
    public Iterable<Metadata<Extension>> getExtensions() {
        return extensions;
    }
}
