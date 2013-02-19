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
package org.ops4j.pax.cdi.web.impl;

import javax.servlet.ServletContainerInitializer;

import org.ops4j.pax.web.service.WebAppDependencyHolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

/**
 * Holds the runtime dependencies of a web bean bundle. For each web bean bundle, an instance
 * of this class is registered an an OSGi service with property {@code bundle.id} set to the
 * bundle ID of the web bean bundle.
 * <p>
 * The service is unregistered as soon as one of the dependencies becomes unavailable.
 * <p>
 * The collaboration of Pax CDI and Pax Web in the startup phase of a web bean bundle is
 * synchronized by means of this service.
 * 
 * @author Harald Wellmann
 */
public class CdiWebAppDependencyHolder implements WebAppDependencyHolder {

    private BundleContext context;
    private ServletContainerInitializer initializer;
    private HttpService httpService;

    public CdiWebAppDependencyHolder(BundleContext context, ServletContainerInitializer initializer) {
        this.context = context;
        this.initializer = initializer;
    }

    @Override
    public HttpService getHttpService() {
        if (httpService == null) {
            ServiceReference<HttpService> httpServiceRef = context.getServiceReference(HttpService.class);
            httpService = context.getService(httpServiceRef);
        }
        return httpService;
    }

    @Override
    public ServletContainerInitializer getServletContainerInitializer() {
        return initializer;
    }

}
