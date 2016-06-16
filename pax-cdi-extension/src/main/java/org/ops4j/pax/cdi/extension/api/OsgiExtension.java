/*
 * Copyright 2016 Guillaume Nodet
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
package org.ops4j.pax.cdi.extension.api;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import java.lang.annotation.Annotation;
import java.util.Set;

import org.ops4j.pax.cdi.extension.impl.OsgiExtension2;

/**
 * Public API for programmatic access to the OSGi extension
 */
public class OsgiExtension implements Extension {

    BeanManager manager;

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager manager) {
        this.manager = manager;
    }

    /**
     * Programmatically add a @Global dependency on specific service class that
     * needs to be available in the OSGi registry before the @Component beans
     * are initialized.
     */
    public <T> Bean<T> globalDependency(Class<T> clazz, Set<Annotation> qualifiers) {
        return manager.getExtension(OsgiExtension2.class)
                .globalDependency(clazz, qualifiers);
    }

}
