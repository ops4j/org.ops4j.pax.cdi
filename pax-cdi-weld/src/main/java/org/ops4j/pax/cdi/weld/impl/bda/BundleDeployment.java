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

import java.util.Collections;
import java.util.List;
import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.ops4j.pax.cdi.spi.scan.BeanScanner;
import org.ops4j.pax.cdi.weld.impl.WeldParser;
import org.osgi.framework.Bundle;

/**
 * Implements {@link CDI11Deployment} for bean bundles, keeping track of the given bundle.
 *
 * @author Harald Wellmann
 *
 */
public class BundleDeployment implements CDI11Deployment {

    private ServiceRegistry serviceRegistry;

    private Iterable<Metadata<Extension>> extensions;
    private BundleBeanDeploymentArchive beanDeploymentArchive;

    /**
     * Creates a bundle deplyoment for the given bundle.
     *
     * @param bundle
     *            bean bundle
     * @param bootstrap
     *            Weld bootstrap
     * @param extensionClassLoader
     *            extended bundle class loader covering the required extensions
     */
    public BundleDeployment(Bundle bundle, Bootstrap bootstrap, ClassLoader extensionClassLoader) {
        serviceRegistry = new SimpleServiceRegistry();
        extensions = bootstrap.loadExtensions(extensionClassLoader);

        createBeanDeploymentArchive(bundle, extensionClassLoader);
    }

    private void createBeanDeploymentArchive(Bundle bundle, ClassLoader extensionClassLoader) {
        WeldParser parser = new WeldParser();
        BeanScanner scanner = new BeanScanner(bundle, parser);
        scanner.scan();
        beanDeploymentArchive = new BundleBeanDeploymentArchive("pax-cdi-bda"
            + bundle.getBundleId());
        beanDeploymentArchive.setBeansXml(parser.getModel());

        beanDeploymentArchive.setBeanClasses(scanner.getBeanClasses());
        ResourceLoader loader = new ClassLoaderResourceLoader(extensionClassLoader);
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

    @Override
    public BeanDeploymentArchive getBeanDeploymentArchive(Class<?> beanClass) {
        return beanDeploymentArchive;
    }
}
