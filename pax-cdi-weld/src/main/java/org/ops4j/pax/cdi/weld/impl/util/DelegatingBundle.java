/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ops4j.pax.cdi.weld.impl.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Bundle that delegates ClassLoader operations to a collection of {@link Bundle} objects.
 *
 * @version $Rev: 1371482 $ $Date: 2012-08-09 13:58:28 -0700 (Thu, 09 Aug 2012) $
 */
class DelegatingBundle implements Bundle {

    private static final String PACKAGE_CACHE = DelegatingBundle.class.getName() + ".packageCache";    
    private static final String RESOURCE_CACHE_SIZE = DelegatingBundle.class.getName() + ".resourceCacheSize";
    
    private static final URL NOT_FOUND_RESOURCE;
    
    static {
        try {
            NOT_FOUND_RESOURCE = new URL("file://foo");
        }
        catch (MalformedURLException e) {
            throw new Error(e);
        }        
    }
    
    private Set<Bundle> bundles;
    private Bundle bundle;
    private BundleContext bundleContext;

    private final boolean hasDynamicImports;
    private final Map<String, URL> resourceCache;
    private final boolean packageCacheEnabled;
    private Map<String, Bundle> packageCache;
    
    DelegatingBundle(Collection<Bundle> bundles) {
        if (bundles.isEmpty()) {
            throw new IllegalArgumentException("At least one bundle is required");
        }
        this.bundles = Collections.newSetFromMap(new ConcurrentHashMap<Bundle, Boolean>());
        this.bundles.addAll(bundles);
        Iterator<Bundle> iterator = bundles.iterator();
        // assume first Bundle is the main bundle
        this.bundle = iterator.next();
        this.bundleContext = new DelegatingBundleContext(this, bundle.getBundleContext());
        this.hasDynamicImports = hasDynamicImports(iterator);
        this.resourceCache = initResourceCache();
        this.packageCacheEnabled = initPackageCacheEnabled();
    }

    DelegatingBundle(Bundle bundle) {
        this(Collections.singletonList(bundle));
    }
    
    private static Map<String, URL> initResourceCache() {
        String value = System.getProperty(RESOURCE_CACHE_SIZE, "250");
        int size = Integer.parseInt(value);
        if (size > 0) {
            return Collections.synchronizedMap(new Cache<String, URL>(size));
        }
        else {
            return null;
        }
    }
    
    private static boolean initPackageCacheEnabled() {
        String value = System.getProperty(PACKAGE_CACHE, "true");
        boolean enabled = Boolean.parseBoolean(value);
        return enabled;
    }
    
    /*
     * Returns true if a single bundle has Dynamic-ImportPackage: *. False, otherwise.       
     */
    private boolean hasDynamicImports(Iterator<Bundle> iterator) {
        while (iterator.hasNext()) {
            Bundle delegate = iterator.next();
            if (hasWildcardDynamicImport(delegate)) {
                return true;
            }
        }
        return false;
    }
    
    private synchronized Map<String, Bundle> getPackageBundleMap() {
        if (packageCache == null) {
            packageCache = buildPackageBundleMap();
        }
        return packageCache;
    }
    
    private synchronized void reset() {
        resourceCache.clear();
        packageCache = null;
    }

    private Map<String, Bundle> buildPackageBundleMap() {
        Map<String, Bundle> map = new HashMap<String, Bundle>();
        Iterator<Bundle> iterator = bundles.iterator();
        // skip first bundle
        iterator.next();
        // attempt to load the class from the remaining bundles
        while (iterator.hasNext()) {
            Bundle b = iterator.next();
            BundleWiring wiring = b.adapt(BundleWiring.class);
            if (wiring != null) {
                List<BundleCapability> capabilities = wiring.getCapabilities(BundleRevision.PACKAGE_NAMESPACE);
                if (capabilities != null && !capabilities.isEmpty()) {
                    for (BundleCapability capability : capabilities) {
                        Map<String, Object> attributes = capability.getAttributes();
                        if (attributes != null) {
                            String packageName = String.valueOf(attributes.get(BundleRevision.PACKAGE_NAMESPACE));
                            if (!map.containsKey(packageName)) {
                                map.put(packageName, b);
                            }
                        }
                    }
                }
            }
        }
        return map;
    }
    
    public Bundle getMainBundle() {
        return bundle;
    }
        
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            Class<?> clazz = bundle.loadClass(name);
            return clazz;
        }
        catch (ClassNotFoundException cnfe) {
            if (name.startsWith("java.")) {
                throw cnfe;
            }
            
            int index = name.lastIndexOf('.');
            if (index > 0 && bundles.size() > 1) {
                String packageName = name.substring(0, index);
                if (packageCacheEnabled) {
                    return findCachedClass(name, packageName, cnfe);
                }
                else {
                    return findClass(name, packageName, cnfe);
                }
            }
            
            throw cnfe;
        }
    }
    
    private Class<?> findCachedClass(String className, String packageName, ClassNotFoundException cnfe) throws ClassNotFoundException {
        Map<String, Bundle> map = getPackageBundleMap();
        Bundle b = map.get(packageName);
        if (b == null) {
            // Work-around for Introspector always looking for classes in sun.beans.infos
            if (packageName.equals("sun.beans.infos") && className.endsWith("BeanInfo")) {
                throw cnfe;
            }
            return findClass(className, packageName, cnfe);
        }
        else {
            return b.loadClass(className);
        }
    }
        
    private Class<?> findClass(String className, String packageName, ClassNotFoundException cnfe) throws ClassNotFoundException {
        Iterator<Bundle> iterator = bundles.iterator();
        // skip first bundle
        iterator.next();
        while (iterator.hasNext()) {
            Bundle delegate = iterator.next();
            if (hasDynamicImports && hasWildcardDynamicImport(delegate)) {
                // skip any bundles with Dynamic-ImportPackage: * to avoid unnecessary wires
                continue;
            }
            try {
                return delegate.loadClass(className);
            }
            catch (ClassNotFoundException e) {
                // ignore
            }
        }
        throw cnfe;
    }
      
    private static boolean hasWildcardDynamicImport(Bundle bundle) {
        Dictionary<String, String> headers = bundle.getHeaders();
        if (headers != null) {
            String value = headers.get(Constants.DYNAMICIMPORT_PACKAGE);
            if (value == null) {
                return false;
            }
            else {
                return "*".equals(value.trim());
            }
        }
        else {
            return false;
        }
    }
    
    public void addBundle(Bundle b) {
        if (bundles.add(b)) {
            reset();
        }
    }

    public void removeBundle(Bundle b) {
        if (bundles.remove(b)) {
            reset();
        }
    }

    public URL getResource(String name) {
        URL resource;
        if (resourceCache == null) {
            resource = findResource(name);
        }
        else {
            resource = findCachedResource(name);
        }
        return resource;
    }
    
    private URL findCachedResource(String name) {
        URL resource = bundle.getResource(name);
        if (resource == null) {
            resource = resourceCache.get(name);
            if (resource == null) {
                Iterator<Bundle> iterator = bundles.iterator();
                // skip first bundle
                iterator.next();
                // look for resource in the remaining bundles
                resource = findResource(name, iterator);                
                resourceCache.put(name, (resource == null) ? NOT_FOUND_RESOURCE : resource);
            }
            else if (resource == NOT_FOUND_RESOURCE) {
                resource = null;
            }
        }
        return resource;
    }
    
    private URL findResource(String name) {
        Iterator<Bundle> iterator = bundles.iterator();
        return findResource(name, iterator);
    }
    
    private URL findResource(String name, Iterator<Bundle> iterator) {
        URL resource = null;
        while (iterator.hasNext() && resource == null) {
            Bundle delegate = iterator.next();
            resource = delegate.getResource(name);
        }
        return resource;
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        ArrayList<URL> allResources = new ArrayList<URL>();
        for (Bundle b : bundles) {
            Enumeration<URL> e = b.getResources(name);
            addToList(allResources, e);
        }
        return Collections.enumeration(allResources);
    }

    private static void addToList(List<URL> list, Enumeration<URL> enumeration) {
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                list.add(enumeration.nextElement());
            }
        }
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public Enumeration findEntries(String arg0, String arg1, boolean arg2) {
        return bundle.findEntries(arg0, arg1, arg2);
    }

    public long getBundleId() {
        return bundle.getBundleId();
    }

    public URL getEntry(String arg0) {
        return bundle.getEntry(arg0);
    }

    public Enumeration getEntryPaths(String arg0) {
        return bundle.getEntryPaths(arg0);
    }

    public Dictionary getHeaders() {
        return bundle.getHeaders();
    }

    public Dictionary getHeaders(String arg0) {
        return bundle.getHeaders(arg0);
    }

    public long getLastModified() {
        return bundle.getLastModified();
    }

    public String getLocation() {
        return bundle.getLocation();
    }

    public ServiceReference[] getRegisteredServices() {
        return bundle.getRegisteredServices();
    }

    public ServiceReference[] getServicesInUse() {
        return bundle.getServicesInUse();
    }

    public Map getSignerCertificates(int arg0) {
        return bundle.getSignerCertificates(arg0);
    }

    public int getState() {
        return bundle.getState();
    }

    public String getSymbolicName() {
        return bundle.getSymbolicName();
    }

    public Version getVersion() {
        return bundle.getVersion();
    }

    public boolean hasPermission(Object arg0) {
        return bundle.hasPermission(arg0);
    }

    public void start() throws BundleException {
        bundle.start();
    }

    public void start(int arg0) throws BundleException {
        bundle.start(arg0);
    }

    public void stop() throws BundleException {
        bundle.stop();
    }

    public void stop(int arg0) throws BundleException {
        bundle.stop(arg0);
    }

    public void uninstall() throws BundleException {
        bundle.uninstall();
    }

    public void update() throws BundleException {
        bundle.update();
    }

    public void update(InputStream arg0) throws BundleException {
        bundle.update(arg0);
    }

    public int compareTo(Bundle other) {
        return bundle.compareTo(other);
    }

    public <A> A adapt(Class<A> type) {
        return bundle.adapt(type);
    }

    public File getDataFile(String filename) {
        return bundle.getDataFile(filename);
    }
    
    public String toString() {
        return "[DelegatingBundle: " + bundles + "]";
    }
    
    private static class Cache<K, V> extends LinkedHashMap<K, V> {
        
        private final int maxSize;
        
        Cache(int maxSize) {
            this(16, maxSize, 0.75f);
        }
        
        Cache(int initialSize, int maxSize, float loadFactor) {
            super(initialSize, loadFactor, true);
            this.maxSize = maxSize;
        }
        
        @Override   
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }

}
