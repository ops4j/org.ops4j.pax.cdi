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
package org.ops4j.pax.cdi.undertow.weld.impl;

import javax.servlet.ServletContextListener;

import org.ops4j.pax.cdi.spi.CdiWebAdapter;
import org.ops4j.pax.cdi.web.AbstractWebAppDependencyManager;
import org.osgi.service.component.annotations.Component;

/**
 * Pax CDI Adapter for Weld using the Undertow servlet container. This adapter provides
 * a servlet container initializer to be picked up by Undertow.
 *
 * @author Harald Wellmann
 *
 */
@Component(service = CdiWebAdapter.class)
public class WeldUndertowAdapter extends AbstractWebAppDependencyManager {

    @Override
    public ServletContextListener createServletContextListener() {
        return new WeldServletContextListener();
    }

}
