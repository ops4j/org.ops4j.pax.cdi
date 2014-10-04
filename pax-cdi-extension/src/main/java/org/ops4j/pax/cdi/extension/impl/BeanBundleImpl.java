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
package org.ops4j.pax.cdi.extension.impl;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.ops4j.pax.cdi.api.ContainerInitialized;
import org.ops4j.pax.cdi.extension.impl.component.ComponentLifecycleManager;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CDI bean representing a CDI-enabled OSGi bundle, or bean bundle, for short.
 * <p>
 * Not intended to be used by application code. This bean is used internally to observe the
 * ContainerInitialized event and then to publish CDI beans as OSGi services.
 *
 * @author Harald Wellmann
 *
 */
@ApplicationScoped
public class BeanBundleImpl {

    private static Logger log = LoggerFactory.getLogger(BeanBundleImpl.class);

    @Inject
    private BundleContext bundleContext;

    @Inject
    private ComponentLifecycleManager componentLifecycleManager;

    /**
     * Register OSGi services when the bean is initialized
     */
    public void onInitialized(@Observes ContainerInitialized event) {
        log.debug("onInitialized {}", bundleContext.getBundle());
        componentLifecycleManager.start();
    }

    /**
     * Unregister OSGi services when the bean is destroyed
     */
    @PreDestroy
    public void onDestroy() {
        log.debug("onDestroy {}", bundleContext.getBundle());
        componentLifecycleManager.stop();
    }
}
