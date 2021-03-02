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
package org.ops4j.pax.cdi.spi.scan;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.xbean.finder.archive.Archive;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleArchive implements Archive {

    private static Logger log = LoggerFactory.getLogger(BundleArchive.class);

    private static final String CLASS_EXT = ".class";

    private Bundle bundle;
    private Map<String, Entry> entries;
    private BundleFilter filter;

    private static class BundleArchiveEntry implements Entry {

        private Bundle provider;
        private URL path;
        private String name;

        BundleArchiveEntry(Bundle provider, URL path, String name) {
            this.provider = provider;
            this.path = path;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public InputStream getBytecode() throws IOException {
            return path.openStream();
        }

        public Bundle getProvider() {
            return provider;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof BundleArchiveEntry)) {
                return false;
            }
            BundleArchiveEntry other = (BundleArchiveEntry) obj;
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            }
            else if (!name.equals(other.name)) {
                return false;
            }
            return true;
        }
    }

    public BundleArchive(Bundle bundle) {
        this(bundle, new DefaultBundleFilter());
    }

    public BundleArchive(Bundle bundle, BundleFilter filter) {
        this.bundle = bundle;
        this.entries = new HashMap<>();
        this.filter = filter;
    }

    @Override
    public Iterator<Entry> iterator() {
        entries = new HashMap<>();
        for (String name : bundle.adapt(BundleWiring.class).listResources(
                "/", "*.class",
                BundleWiring.LISTRESOURCES_LOCAL | BundleWiring.LISTRESOURCES_RECURSE)) {
            String klass = toClassName("", name);
            if (filter.accept(bundle, klass)) {
                URL url = bundle.getResource(name);
                BundleArchiveEntry archiveEntry = new BundleArchiveEntry(bundle, url, klass);
                entries.put(klass, archiveEntry);
            }
        }
        return entries.values().iterator();
    }

    @Override
    public InputStream getBytecode(String className) throws IOException, ClassNotFoundException {
        Entry entry = entries.get(className);
        if (entry == null) {
            return null;
        }
        return entry.getBytecode();
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return bundle.loadClass(className);
    }

    public Bundle getProvider(String className) {
        BundleArchiveEntry entry = (BundleArchiveEntry) entries.get(className);
        if (entry == null) {
            return null;
        }
        return entry.getProvider();
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

        klass = klass.replace("/", ".").replace(CLASS_EXT, "");
        log.trace("file = {}, class = {}", file, klass);
        return klass;
    }

}
