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

import java.net.URL;
import java.util.Collections;
import java.util.List;


public class DefaultBeanDescriptor implements BeanDescriptor {

    private URL url;
    private BeanDiscoveryMode mode;
    private String version;


    public DefaultBeanDescriptor(URL url) {
        this(url, BeanDiscoveryMode.ANNOTATED, "1.1");
    }

    public DefaultBeanDescriptor(URL url, BeanDiscoveryMode mode, String version) {
        this.url = url;
        this.mode = mode;
        this.version = version;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public BeanDiscoveryMode getBeanDiscoveryMode() {
        return mode;
    }

    @Override
    public boolean isClassExcluded(String clazz) {
        return false;
    }

    @Override
    public boolean isPackageExcluded(String packageName) {
        return false;
    }

    @Override
    public List<String> getInterceptors() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getDecorators() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getAlternativeClasses() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getAlternativeStereotypes() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getExcludedClasses() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getExcludedPackages() {
        return Collections.emptyList();
    }

    @Override
    public URL getUrl() {
        return url;
    }
}
