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
package org.ops4j.pax.cdi.openwebbeans.impl;

import java.net.URL;
import java.util.List;

import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.spi.BeanArchiveService.BeanArchiveInformation;
import org.ops4j.pax.cdi.spi.scan.BeanDescriptor;
import org.ops4j.pax.cdi.spi.scan.BeanDescriptorParser;
import org.ops4j.pax.cdi.spi.scan.BeanDiscoveryMode;


public class OpenWebBeansParser implements BeanDescriptorParser {


    private BeanArchiveService archiveService;
    private BeanArchiveInformation archiveInfo;

    private class BeanDescriptorImpl implements BeanDescriptor {


        private URL url;

        BeanDescriptorImpl(URL url) {
            this.url = url;
        }

        @Override
        public String getVersion() {
            return archiveInfo.getVersion();
        }

        @Override
        public BeanDiscoveryMode getBeanDiscoveryMode() {
            return BeanDiscoveryMode.valueOf(archiveInfo.getBeanDiscoveryMode().toString());
        }

        @Override
        public boolean isClassExcluded(String clazz) {
            return archiveInfo.isClassExcluded(clazz);
        }

        @Override
        public boolean isPackageExcluded(String packageName) {
            return archiveInfo.isPackageExcluded(packageName);
        }

        @Override
        public List<String> getInterceptors() {
            return archiveInfo.getInterceptors();
        }

        @Override
        public List<String> getDecorators() {
            return archiveInfo.getDecorators();
        }

        @Override
        public List<String> getAlternativeClasses() {
            return archiveInfo.getAlternativeClasses();
        }

        @Override
        public List<String> getAlternativeStereotypes() {
            return archiveInfo.getAlternativeStereotypes();
        }

        @Override
        public List<String> getExcludedClasses() {
            return archiveInfo.getExcludedClasses();
        }

        @Override
        public List<String> getExcludedPackages() {
            return archiveInfo.getExcludedPackages();
        }

        @Override
        public URL getUrl() {
            return url;
        }
    }

    public OpenWebBeansParser(BeanArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @Override
    public BeanDescriptor parse(URL beansXml) {
        archiveInfo = archiveService.getBeanArchiveInformation(beansXml);
        return new BeanDescriptorImpl(beansXml);
    }
}
