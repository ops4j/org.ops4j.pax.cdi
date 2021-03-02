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
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ops4j.pax.cdi.spi.BeanBundles;
import org.osgi.framework.Bundle;


public class BeanBundleFilter implements BundleFilter {

    private BeanDescriptorParser parser;
    private Map<Bundle, BeanDescriptor> descriptorMap;

    public BeanBundleFilter(BeanDescriptorParser parser) {
        this.parser = parser;
        this.descriptorMap = new HashMap<>();
    }

    @Override
    public boolean accept(Bundle providerBundle, String className) {
        BeanDescriptor descriptor = findDescriptor(providerBundle);
        switch (descriptor.getBeanDiscoveryMode()) {
            case ALL:
            case ANNOTATED:
                return true;
            case NONE:
                return false;
            default:
                throw new IllegalArgumentException(descriptor.getBeanDiscoveryMode().toString());
        }
    }

    public BeanDescriptor findDescriptor(Bundle providerBundle) {
        BeanDescriptor descriptor = descriptorMap.get(providerBundle);
        if (descriptor == null) {
            descriptor = loadDescriptor(providerBundle);
            descriptorMap.put(providerBundle, descriptor);
        }
        return descriptor;
    }

    private BeanDescriptor loadDescriptor(Bundle bundle) {
        URL beansXml = null;
        if (isWebBundle(bundle)) {
            beansXml = bundle.getEntry("WEB-INF/beans.xml");
            if (beansXml == null) {
                beansXml = bundle.getEntry("WEB-INF/classes/META-INF/beans.xml");
            }
        }
        else {
            beansXml = bundle.getEntry("META-INF/beans.xml");
        }
        if (beansXml == null) {
            if (BeanBundles.isBeanBundle(bundle)) {
                return new DefaultBeanDescriptor(bundle.getEntry("/"));
            }
            else {
                return new DefaultBeanDescriptor(bundle.getEntry("/"), BeanDiscoveryMode.NONE, "1.1");
            }
        }
        return parser.parse(beansXml);
    }

    private boolean isWebBundle(Bundle bundle) {
        if (bundle != null) {
            if (bundle.getHeaders() != null) {
                Dictionary<String, String> headers = bundle.getHeaders();
                String contextPath = headers.get("Web-ContextPath");
                return (contextPath != null);
            }
        }
        return false;
    }

    public List<URL> getBeanDescriptors() {
        List<URL> urls = new ArrayList<>();
        for (BeanDescriptor descriptor : descriptorMap.values()) {
            urls.add(descriptor.getUrl());
        }
        return urls;
    }
}
