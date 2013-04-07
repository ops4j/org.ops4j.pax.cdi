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
package org.ops4j.pax.cdi.spi.scan;

import static org.osgi.framework.Constants.BUNDLE_CLASSPATH;
import static org.osgi.framework.wiring.BundleRevision.BUNDLE_NAMESPACE;
import static org.osgi.framework.wiring.BundleRevision.PACKAGE_NAMESPACE;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.ops4j.pax.cdi.spi.BeanBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans a bundle for candidate managed bean classes. The scanner only looks at bundle entries but
 * does not load classes. The set of candidate classes is passed to the CDI implementation which
 * will discard some of the candidates, e.g. if the class cannot be loaded or does not have a
 * default constructor.
 * <p>
 * The scanner returns all classes contained in the given bundle, including embedded archives and
 * directories from the bundle classpath, and all classes visible from required bundle wires
 * (package imports or required bundles), provided that the exporting bundle is a bean bundle.
 * 
 * @author Harald Wellmann
 * 
 */
public class BeanScanner {

    private static final String CLASS_EXT = ".class";
    
    private static final String[] BEAN_DESCRIPTOR_PATHS = new String[]  {
      "META-INF/beans.xml",
      "WEB-INF/beans.xml"
    };

    private Logger log = LoggerFactory.getLogger(BeanScanner.class);

    private Bundle bundle;

    private Set<URL> beanDescriptors;
    private Set<String> beanClasses;
    
    private Set<String> scannedPackages;

    /**
     * Constructs a bean scanner for the given bundle.
     * 
     * @param bundle
     *            bundle to be scanned
     */
    public BeanScanner(Bundle bundle) {
        this.bundle = bundle;
        this.beanDescriptors = new HashSet<URL>();
        this.beanClasses = new TreeSet<String>();
    }

    /**
     * Returns the class names of all bean candidate classes.
     * 
     * @return unmodifiable set
     */
    public Set<String> getBeanClasses() {
        return Collections.unmodifiableSet(beanClasses);
    }

    /**
     * Return the URLs of all bean descriptors (beans.xml).
     * 
     * @return unmodifiable set
     */
    public Set<URL> getBeanDescriptors() {
        return Collections.unmodifiableSet(beanDescriptors);
    }

    /**
     * Scans the given bundle and all imports for bean classes.
     */
    public void scan() {
        scannedPackages = new HashSet<String>();
        scanOwnBundle();
        scanImportedPackages();
        scanRequiredBundles();
        logBeanClasses();
    }

    private void logBeanClasses() {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("candidate bean classes for bundle [{}]:", bundle);
        for (String klass : beanClasses) {
            log.debug("    {}", klass);
        }
    }

    private void scanOwnBundle() {
        findBeanDescriptors();
        
        String[] classPathElements;

        String bundleClassPath = bundle.getHeaders().get(BUNDLE_CLASSPATH);
        if (bundleClassPath == null) {
            classPathElements = new String[] { "/" };
        }
        else {
            classPathElements = bundleClassPath.split(",");
        }

        for (String cp : classPathElements) {
            String classPath = cp;
            if (classPath.equals(".")) {
                classPath = "/";
            }

            if (classPath.endsWith(".jar") || classPath.endsWith(".zip")) {
                scanZip(classPath);
            }
            else {
                scanDirectory(classPath);
            }
        }
    }

    private void findBeanDescriptors() {
        for (String path : BEAN_DESCRIPTOR_PATHS) {
            URL url = bundle.getEntry(path);
            if (url != null) {
                beanDescriptors.add(url);
                return;
            }            
        }
    }

    private void scanDirectory(String classPath) {
        Enumeration<URL> e = bundle.findEntries(classPath, "*.class", true);
        while (e != null && e.hasMoreElements()) {
            URL url = e.nextElement();
            String klass = toClassName(classPath, url);
            beanClasses.add(klass);
        }
    }

    private void scanZip(String zipName) {
        URL zipEntry = bundle.getEntry(zipName);
        if (zipEntry == null) {
            return;
        }
        ZipInputStream in = null;
        try {
            in = new ZipInputStream(zipEntry.openStream());
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith(CLASS_EXT)) {
                    beanClasses.add(toClassName("", name));
                }
            }
        }
        catch (IOException exc) {
            log.warn("error scanning zip file " + zipName, exc);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private String toClassName(String classPath, URL url) {
        return toClassName(classPath, url.getFile());
    }

    private String toClassName(String classPath, String file) {
        String klass = null;
        String[] parts = file.split("!");
        if (parts.length > 1) {
            klass = parts[1];
        }
        else {
            klass = file;
        }
        if (klass.charAt(0) == '/') {
            klass = klass.substring(1);
        }

        String prefix = classPath;
        if (classPath.length() > 1) {
            if (classPath.charAt(0) == '/') {
                prefix = classPath.substring(1);
            }
            assert klass.startsWith(prefix);
            int startIndex = prefix.length();
            if (!prefix.endsWith("/")) {
                startIndex++;
            }
            klass = klass.substring(startIndex);
        }

        klass = klass.replace("/", ".").replace(".class", "");
        log.trace("file = {}, class = {}", file, klass);
        return klass;
    }

    private void scanImportedPackages() {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        List<BundleWire> wires = wiring.getRequiredWires(PACKAGE_NAMESPACE);
        for (BundleWire wire : wires) {
            log.debug("scanning imported package [{}]", wire);
            scanForClasses(wire);
        }
    }

    private void scanForClasses(BundleWire wire) {
        BundleWiring wiring = wire.getProviderWiring();
        Bundle providerBundle = wiring.getBundle();
        if (!BeanBundles.isBeanBundle(providerBundle)) {
            return;
        }
        scanExportedPackage(wiring, wire.getCapability());
    }
    
    private void scanExportedPackage(BundleWiring wiring, BundleCapability capability) {
        String pkg = (String) capability.getAttributes().get(PACKAGE_NAMESPACE);
        if (scannedPackages.contains(pkg)) {
            return;
        }
        
        log.debug("scanning exported package [{}]", pkg);
        scannedPackages.add(pkg);
        Collection<String> entries = wiring.listResources(toPath(pkg), "*.class",
            BundleWiring.LISTRESOURCES_LOCAL);
        for (String entry : entries) {
            beanClasses.add(toClassName("", entry));
        }        
    }

    private String toPath(String pkg) {
        return pkg.replaceAll("\\.", "/");
    }

    private void scanRequiredBundles() {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        List<BundleWire> wires = wiring.getRequiredWires(BUNDLE_NAMESPACE);
        for (BundleWire wire : wires) {
            BundleWiring providerWiring = wire.getProviderWiring();
            log.debug("scanning required bundle [{}]", providerWiring.getBundle());
            List<BundleCapability> capabilities = providerWiring.getCapabilities(PACKAGE_NAMESPACE);
            for (BundleCapability pkgCapability : capabilities) {
                scanExportedPackage(providerWiring, pkgCapability);
            }
        }
    }
}
