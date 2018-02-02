/*
 * Copyright 2014 Harald Wellmann.
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
package org.ops4j.pax.cdi.test.osgi6;

import static org.ops4j.pax.cdi.test.support.TestConfiguration.cdiProviderBundles;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.sample5.Client;
import org.ops4j.pax.cdi.sample5.SingletonScopedService;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@Ignore
public class ServiceEventTest {

    @Inject
    private BundleContext bc;

    @Inject
    @Filter(value = "(name=client11)")
    private Client client11;

    @Inject
    @Filter(value = "(name=client12)")
    private Client client12;

    @Inject
    @Filter(value = "(name=client21)")
    private Client client21;

    @Inject
    @Filter(value = "(name=client22)")
    private Client client22;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),

            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample5"),
            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample5-client1"),
            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample5-client2"),
            paxCdiProviderAdapter(),
            cdiProviderBundles());
    }

    @Test
    public void shouldObserveServiceEvent() throws InterruptedException {
        SingletonScopedService myService = new SingletonScopedService() {

            @Override
            public int getNumber() {
                return -1;
            }

        };

        bc.registerService(SingletonScopedService.class, myService, null);
        Thread.sleep(3000);
    }
}
