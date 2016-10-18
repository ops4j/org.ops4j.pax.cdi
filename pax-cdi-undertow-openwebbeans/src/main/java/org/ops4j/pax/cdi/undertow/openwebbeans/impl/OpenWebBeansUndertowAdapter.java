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
package org.ops4j.pax.cdi.undertow.openwebbeans.impl;

import javax.servlet.ServletContextListener;

import org.apache.webbeans.config.WebBeansFinder;
import org.ops4j.pax.cdi.spi.CdiContainerListener;
import org.ops4j.pax.cdi.web.AbstractWebAppDependencyManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Web adapter for OpenWebBeans.
 *
 * @author Harald Wellmann
 *
 */
@Component(immediate = true, service = CdiContainerListener.class)
public class OpenWebBeansUndertowAdapter extends AbstractWebAppDependencyManager {

    /**
     * Called by the OSGi framework when this bundle is activated. Registers a custom
     * singleton service.
     */
    @Activate
    public void activate() {
        WebBeansFinder.setSingletonService(new BundleSingletonService());
    }

    @Override
    public ServletContextListener createServletContextListener() {
        return new OpenWebBeansListener();
    }

}
