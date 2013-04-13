/*
 * Copyright 2013 Harald Wellmann
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

package org.ops4j.pax.cdi.sample1;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.ops4j.pax.cdi.api.event.BundleCdiEvent;
import org.ops4j.pax.cdi.api.event.BundleStarted;
import org.ops4j.pax.cdi.api.event.BundleStopped;


/**
 * @author Harald Wellmann
 *
 */
@ApplicationScoped
public class BundleEventObserver {
    
    private List<BundleCdiEvent> bundleStartedEvents = new ArrayList<BundleCdiEvent>();
    private List<BundleCdiEvent> bundleStoppedEvents = new ArrayList<BundleCdiEvent>();
    
    public void bundleStarted(@Observes @BundleStarted BundleCdiEvent event) {
        bundleStartedEvents.add(event);
    }

    public void bundleStopped(@Observes @BundleStopped BundleCdiEvent event) {
        bundleStoppedEvents.add(event);
    }

    
    public List<BundleCdiEvent> getBundleStartedEvents() {
        return bundleStartedEvents;
    }
    
    public List<BundleCdiEvent> getBundleStoppedEvents() {
        return bundleStoppedEvents;
    }
}
