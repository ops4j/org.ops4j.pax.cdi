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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.webbeans.spi.LoaderService;
import org.ops4j.lang.Ops4jException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BundleLoaderService implements LoaderService {
    
    private static Logger log = LoggerFactory.getLogger(BundleLoaderService.class);

    @Override
    public <T> List<T> load(Class<T> serviceType) {
        return load(serviceType, Thread.currentThread().getContextClassLoader());
    }

    @Override
    public <T> List<T> load(Class<T> serviceType, ClassLoader classLoader) {
        List<T> services = new ArrayList<T>();
        String resourceName = "/META-INF/services/" + serviceType.getName();
        try {
            Enumeration<URL> resources = classLoader.getResources(resourceName);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                List<String> classNames = parse(url);
                for (String className : classNames) {
                    Class<T> klass = loadClassIfVisisble(className, classLoader);
                    if (klass != null) {
                        T service = klass.newInstance();
                        services.add(service);
                    }
                }
            }
        }
        catch (IOException exc) {
            throw new Ops4jException(exc);
        }
        catch (InstantiationException exc) {
            throw new Ops4jException(exc);
        }
        catch (IllegalAccessException exc) {
            throw new Ops4jException(exc);
        }
        return services;
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> loadClassIfVisisble(String className, ClassLoader classLoader) {
        try {
            Class<T> klass = (Class<T>) classLoader.loadClass(className);
            return klass;
        }
        catch (ClassNotFoundException e) {
            return null;
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
}
