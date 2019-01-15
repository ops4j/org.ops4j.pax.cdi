/*
 * Copyright 2013 Harald Wellmann.
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

import java.util.Dictionary;
import java.util.Hashtable;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.sample1.IceCreamService;
import org.ops4j.pax.cdi.sample1.client.IceCreamClient;
import org.ops4j.pax.cdi.sample1.client.StrawberryClient;
import org.ops4j.pax.cdi.sample1.client.StrawberryService;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.swissbox.tracker.ServiceLookup;
import org.ops4j.pax.swissbox.tracker.ServiceLookupException;
import org.osgi.framework.ServiceRegistration;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.cdiProviderBundles;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ComponentLifecycleTest extends AbstractControlledTestBase {

    @Inject
    private IceCreamClient client;

    @Configuration
    public Option[] config() {
        return combine(
                baseConfigure(),
//                regressionDefaults(),

                workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample1"),
                workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample1-client"),

                paxCdiProviderAdapter()
        );
    }

    @Test(expected = ServiceLookupException.class)
    public void componentWithUnsatisfiedDependencyShouldNotBeRegistered(){
        ServiceLookup.getService(bc, StrawberryClient.class, 10);
    }

    @Test
    public void componentShouldRegisterWhenDependencyAppears(){
        ServiceRegistration<IceCreamService> reg = registerStrawberryService();
        StrawberryClient strawberryClient = ServiceLookup.getService(bc, StrawberryClient.class, 100);
        assertThat(strawberryClient, is(notNullValue()));
        assertThat(strawberryClient.getFlavour(), is("Strawberry"));

        reg.unregister();
        assertThat(bc.getServiceReference(StrawberryClient.class), is(nullValue()));
    }

    private ServiceRegistration<IceCreamService> registerStrawberryService() {
        StrawberryService strawberryService = new StrawberryService();
        Dictionary<String,String> props = new Hashtable<String, String>();
        props.put("flavour", "strawberry");
        ServiceRegistration<IceCreamService> reg =
            bc.registerService(IceCreamService.class, strawberryService, props);
        return reg;
    }

    @Test
    public void shouldObserveServiceAddedEvents(){
        registerStrawberryService();
        assertThat(client.getEvents(), hasItems(
            "added IceCreamService with class org.ops4j.pax.cdi.sample1.client.StrawberryService",
            "added IceCreamService with flavour strawberry"
            ));
    }

    @Test
    public void shouldObserveServiceRemovedEvents(){
        ServiceRegistration<IceCreamService> reg = registerStrawberryService();
        reg.unregister();
        assertThat(client.getEvents(), hasItems(
            "removed IceCreamService with class org.ops4j.pax.cdi.sample1.client.StrawberryService",
            "removed IceCreamService with flavour strawberry"));
    }
}
