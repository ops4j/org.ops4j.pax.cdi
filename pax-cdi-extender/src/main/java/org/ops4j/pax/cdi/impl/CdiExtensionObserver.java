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
package org.ops4j.pax.cdi.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.spi.Extension;

import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.swissbox.extender.BundleObserver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observes CDI extension bundles, i.e. all bundles representing a CDI portable extension as
 * indicated by a {@code META-INF/services/javax.enterprise.inject.spi.Extension} resource.
 * 
 * @author Harald Wellmann
 * 
 */
public class CdiExtensionObserver implements BundleObserver<URL> {

    private static Logger log = LoggerFactory.getLogger(CdiExtensionObserver.class);

    /**
     * Maps bundle IDs to CDI extension bundles.
     */
    private Map<Long, Bundle> extensionBundles = new ConcurrentHashMap<Long, Bundle>();

    @Override
    public void addingEntries(Bundle bundle, List<URL> entries) {
        log.info("found CDI extension in bundle {}_{}", bundle.getSymbolicName(),
            bundle.getVersion());

        extensionBundles.put(bundle.getBundleId(), bundle);

        // Parse service provider resource which may contain more than one extension class.
        List<String> names = parse(entries.get(0));
        for (String impl : names) {
            log.info("CDI extension class: {}", impl);
            try {
                // instantiate extension class and publish it as a service
                Class<?> extensionClass = bundle.loadClass(impl);
                Extension extension = (Extension) extensionClass.newInstance();
                BundleContext bc = bundle.getBundleContext();
                bc.registerService(Extension.class.getName(), extension, null);
            }
            catch (ClassNotFoundException exc) {
                throw new Ops4jException(exc);
            }
            catch (InstantiationException exc) {
                throw new Ops4jException(exc);
            }
            catch (IllegalAccessException exc) {
                throw new Ops4jException(exc);
            }
        }
    }

    @Override
    public void removingEntries(Bundle bundle, List<URL> entries) {
        extensionBundles.remove(bundle.getBundleId());
    }

    public Collection<Bundle> getExtensionBundles() {
        return extensionBundles.values();
    }

    private void parseLine(List<String> names, String line) {
        int commentPos = line.indexOf('#');
        if (commentPos >= 0) {
            line = line.substring(0, commentPos);
        }
        line = line.trim();
        if (!line.isEmpty() && !names.contains(line)) {
            names.add(line);
        }
    }

    /**
     * TODO move META-INF/services parser to ops4j-base. This is currently copied from Pax JDBC.
     * 
     * @param klass
     * @param url
     * @return
     */
    private List<String> parse(URL url) {
        InputStream is = null;
        BufferedReader reader = null;
        List<String> names = new ArrayList<String>();
        try {
            is = url.openStream();
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                parseLine(names, line);
            }
        }
        catch (IOException exc) {
            throw new Ops4jException(exc);
        }
        finally {
            closeSilently(reader, url);
        }
        return names;
    }

    private void closeSilently(BufferedReader reader, URL url) {
        try {
            if (reader != null) {
                reader.close();
            }
        }
        catch (IOException exc) {
            log.error("cannot close " + url, exc);
        }
    }
}
