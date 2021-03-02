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
package org.ops4j.pax.cdi.openwebbeans;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.xbean.osgi.bundle.util.BundleUtils;
import org.ops4j.pax.cdi.openwebbeans.impl.OpenWebBeansParser;
import org.ops4j.pax.cdi.spi.scan.BeanScanner;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link ScannerService} for OSGi bundles.
 *
 * @author Harald Wellmann
 *
 */
public class BundleScannerService implements ScannerService {

    private static Logger log = LoggerFactory.getLogger(BundleScannerService.class);

    private BeanScanner scanner;
    private Bundle bundle;
    private Set<Class<?>> beanClasses;

    @Override
    public void init(Object object) {
        // empty
    }

    @Override
    public void scan() {
        BeanArchiveService archiveService = WebBeansContext.getInstance().getBeanArchiveService();
        OpenWebBeansParser parser = new OpenWebBeansParser(archiveService);
        bundle = BundleUtils.getContextBundle(true);
        scanner = new BeanScanner(bundle, parser);
        scanner.scan();
    }

    @Override
    public void release() {
        scanner = null;
        bundle = null;
        beanClasses = null;
    }

    @Override
    public Set<URL> getBeanXmls() {
        return scanner.getBeanDescriptors();
    }

    @Override
    public Set<Class<?>> getBeanClasses() {
        if (beanClasses == null) {
            beanClasses = new HashSet<Class<?>>();
            for (String className : scanner.getBeanClasses()) {
                try {
                    Class<?> klass = bundle.loadClass(className);
                    beanClasses.add(klass);
                }
                catch (ClassNotFoundException exc) {
                    log.debug("cannot load class " + className, exc);
                }
            }
        }
        return beanClasses;
    }

    @Override
    public boolean isBDABeansXmlScanningEnabled() {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public org.apache.webbeans.spi.BDABeansXmlScanner getBDABeansXmlScanner() {
        return null;
    }
}
