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
package org.ops4j.pax.cdi.extension.impl.osgi;

import java.util.ArrayList;
import java.util.List;

import org.ops4j.pax.cdi.extension.api.runtime.CdiOsgiRuntime;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private List<ServiceRegistration<?>> registrations = new ArrayList<>();

    @Override
    public void start(BundleContext context) throws Exception {
        registrations.add(context.registerService(CdiOsgiRuntime.class, Registry.getInstance(), null));
        try {
            registrations.add(CdiBundleState.register(context));
        } catch (Throwable t) {
            // Ignore
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        for (ServiceRegistration reg : registrations) {
            try {
                reg.unregister();
            } catch (Throwable t) {
                // Ignore
            }
        }
    }

}
