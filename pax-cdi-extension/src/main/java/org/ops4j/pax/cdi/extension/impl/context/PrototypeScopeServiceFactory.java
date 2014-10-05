/*
 * Copyright 2014 Harald Wellmann
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

package org.ops4j.pax.cdi.extension.impl.context;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;


public class PrototypeScopeServiceFactory<S> implements PrototypeServiceFactory<S> {


    private PrototypeScopeContext context;
    private Bean<S> bean;

    public PrototypeScopeServiceFactory(PrototypeScopeContext context, Bean<S> bean) {
        this.context = context;
        this.bean = bean;
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public S getService(Bundle bundle, ServiceRegistration<S> registration) {
        CreationalContext<S> cc = (CreationalContext<S>) context.getCreationalContext();
        S service = context.get(bean, cc);
        return service;
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<S> registration, S service) {
        context.setService(service);
        context.destroy(bean);
        context.setService(null);
    }
}
