/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.extender.impl;

import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {

    ServiceTracker<CdiContainerFactory, CdiExtender> tracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        // tracker that transforms CdiContainerFactory into CdiExtender instances
        tracker = new ServiceTracker<>(context, CdiContainerFactory.class, new ServiceTrackerCustomizer<CdiContainerFactory, CdiExtender>() {
            @Override
            public CdiExtender addingService(ServiceReference<CdiContainerFactory> reference) {
                CdiContainerFactory factory = context.getService(reference);
                CdiExtender extender = new CdiExtender(context, factory);
                if (tracker.getService() == null) {
                    // start only first tracked (just after addingService() returns) extender
                    extender.start();
                }
                return extender;
            }
            @Override
            public void modifiedService(ServiceReference<CdiContainerFactory> reference, CdiExtender extender) {
            }
            @Override
            public void removedService(ServiceReference<CdiContainerFactory> reference, CdiExtender extender) {
                extender.stop();
                CdiExtender anotherExtender = tracker.getService();
                if (anotherExtender != null) {
                    // possibly start another tracked extender
                    anotherExtender.start();
                }
            }
        });
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
    }
}
