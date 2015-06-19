/*
 * Copyright 2015 Harald Wellmann.
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
package org.ops4j.pax.cdi.weld.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.xml.BeansXmlParser;
import org.ops4j.pax.cdi.spi.scan.BeanDescriptor;
import org.ops4j.pax.cdi.spi.scan.BeanDescriptorParser;
import org.ops4j.pax.cdi.spi.scan.BeanDiscoveryMode;


public class WeldParser implements BeanDescriptorParser {

    private BeansXml model;

    public static class BeanDescriptorImpl implements BeanDescriptor {

        private URL url;
        private BeansXml model;

        public BeanDescriptorImpl(URL url, BeansXml model) {
            this.url = url;
            this.model = model;
        }

        @Override
        public String getVersion() {
            return model.getVersion();
        }

        @Override
        public BeanDiscoveryMode getBeanDiscoveryMode() {
            return BeanDiscoveryMode.valueOf(model.getBeanDiscoveryMode().toString());
        }

        @Override
        public boolean isClassExcluded(String clazz) {
            // FIXME
            return false;
        }

        @Override
        public boolean isPackageExcluded(String packageName) {
            // FIXME
            return false;
        }

        @Override
        public List<String> getInterceptors() {
            List<String> interceptors = new ArrayList<>();
            for (Metadata<String> interceptor : model.getEnabledInterceptors()) {
                interceptors.add(interceptor.getValue());
            }
            return interceptors;
        }

        @Override
        public List<String> getDecorators() {
            List<String> decorators = new ArrayList<>();
            for (Metadata<String> decorator : model.getEnabledDecorators()) {
                decorators.add(decorator.getValue());
            }
            return decorators;
        }

        @Override
        public List<String> getAlternativeClasses() {
            List<String> klasses = new ArrayList<>();
            for (Metadata<String> klass : model.getEnabledAlternativeClasses()) {
                klasses.add(klass.getValue());
            }
            return klasses;
        }

        @Override
        public List<String> getAlternativeStereotypes() {
            List<String> stereotypes = new ArrayList<>();
            for (Metadata<String> stereotype : model.getEnabledAlternativeStereotypes()) {
                stereotypes.add(stereotype.getValue());
            }
            return stereotypes;
        }

        @Override
        public List<String> getExcludedClasses() {
            // FIXME
            return Collections.emptyList();
        }

        @Override
        public List<String> getExcludedPackages() {
            // FIXME
            return Collections.emptyList();
        }

        @Override
        public URL getUrl() {
            return url;
        }
    }

    @Override
    public BeanDescriptor parse(URL beansXml) {
        BeansXmlParser parser = new BeansXmlParser();
        model = parser.parse(beansXml);
        return new BeanDescriptorImpl(beansXml, model);
    }

    public BeansXml getModel() {
        return model;
    }

}
