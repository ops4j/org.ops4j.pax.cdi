package org.ops4j.pax.cdi.weld.impl.bda;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.ops4j.pax.cdi.weld.impl.OsgiProxyService;
import org.osgi.framework.Bundle;

public class BundleDeployment implements Deployment {

    private ServiceRegistry serviceRegistry;

    private Iterable<Metadata<Extension>> extensions;
    private BundleBeanDeploymentArchive beanDeploymentArchive;

    public BundleDeployment(Bundle bundle, Bootstrap bootstrap) {

        serviceRegistry = new SimpleServiceRegistry();
        serviceRegistry.add(ProxyServices.class, new OsgiProxyService());
        extensions = bootstrap.loadExtensions(Thread.currentThread().getContextClassLoader());

        createBeanDeploymentArchive(bundle, bootstrap);
    }

    private void createBeanDeploymentArchive(Bundle bundle, Bootstrap bootstrap) {
        BundleMetaDataScannerService scanner = new BundleMetaDataScannerService(bundle);
        scanner.scan();
        beanDeploymentArchive = new BundleBeanDeploymentArchive("pax-cdi-bda"
            + bundle.getBundleId());
        beanDeploymentArchive.setBeansXml(bootstrap.parse(scanner.getBeanXmls()));
        Set<String> classes = new HashSet<String>();
        for (Class<?> klass : scanner.getBeanClasses()) {
            classes.add(klass.getName());
        }
        beanDeploymentArchive.setBeanClasses(classes);
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
