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
package org.ops4j.pax.cdi.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.TestConfiguration.cdiProviderBundles;
import static org.ops4j.pax.cdi.test.TestConfiguration.paxCdiProviderAdapter;
import static org.ops4j.pax.cdi.test.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.util.List;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.api.event.BundleCdiEvent;
import org.ops4j.pax.cdi.sample1.BundleEventCollector;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@Ignore("synchronization issues between bundle and CDI lifecycle")
public class BundleEventTest {

    @Inject
    private BundleEventCollector collector;

    @Inject
    private BundleContext bc;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),

            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample1"),
            
            paxCdiProviderAdapter(),            
            cdiProviderBundles());
    }

    @Test
    public void checkBundleEvents() {
        List<BundleCdiEvent> events = collector.getBundleStartedEvents();
        assertThat(events.isEmpty(), is(false));
        for (BundleCdiEvent event : collector.getBundleStartedEvents()) {
            assertThat(event.getBundle(), is(notNullValue()));
            assertThat(event.getBundleEvent().getBundle(), is(event.getBundle()));
            assertThat(event.getBundleEvent().getType(), is(BundleEvent.STARTED));
        }
    }
}
