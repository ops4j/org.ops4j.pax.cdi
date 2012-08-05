package org.ops4j.pax.cdi.web.impl;

import javax.servlet.ServletContainerInitializer;

import org.ops4j.pax.web.service.WebAppDependencyHolder;
import org.osgi.service.http.HttpService;

public class CdiWebAppDependencyHolder implements WebAppDependencyHolder {

    private HttpService httpService;
    private ServletContainerInitializer initializer;

    public CdiWebAppDependencyHolder(HttpService httpService,
        ServletContainerInitializer initializer) {
        this.httpService = httpService;
        this.initializer = initializer;
    }

    @Override
    public HttpService getHttpService() {
        return httpService;
    }

    @Override
    public ServletContainerInitializer getServletContainerInitializer() {
        return initializer;
    }
}
