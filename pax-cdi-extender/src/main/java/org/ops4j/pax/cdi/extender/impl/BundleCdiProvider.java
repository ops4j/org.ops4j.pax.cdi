/*
 * Copyright 2014 Harald Wellmann.
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
package org.ops4j.pax.cdi.extender.impl;

import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.CDIProvider;

import org.ops4j.pax.cdi.spi.BeanBundles;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Implements a {@link CDIProvider} which returns the CDI container of the current bundle,
 * if the bundle is a bean bundle.
 */
class BundleCdiProvider implements CDIProvider {
    
    
    private final CdiContainerFactory cdiContainerFactory;

    BundleCdiProvider(CdiContainerFactory cdiContainerFactory) {
        this.cdiContainerFactory = cdiContainerFactory;
    }

    @Override
    public CDI<Object> getCDI() {
        Bundle bundle = BeanBundles.getCurrentBundle();
        if (bundle == null) {
            return null;
        }
        CdiContainer container = cdiContainerFactory.getContainer(bundle);
        if (container == null) {
            return null;
        }
        
        return new BundleCdi<>(container);
    }

}
