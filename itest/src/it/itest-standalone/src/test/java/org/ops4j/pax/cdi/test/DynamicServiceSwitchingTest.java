/*
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
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.cdiProviderBundles;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.sample7.api.RankedServiceClient;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.swissbox.tracker.ServiceLookupException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class DynamicServiceSwitchingTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Inject
    private CdiContainerFactory containerFactory;

    @Inject
    private BundleContext bc;

    @Inject
    private RankedServiceClient client;

    @Configuration
    public Option[] config() {
        return options(regressionDefaults(),
            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample7-api"),
            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample7-service-impl100"),
            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample7-service-impl101"),
            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample7-service-impl102"),
            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample7-client-dynamic"),
            paxCdiProviderAdapter(), cdiProviderBundles());
    }

    @Test
    public void checkContainerFactory() {
        assertThat(containerFactory.getContainers().size(), is(4));
    }

    @Test
    public void checkInitialServiceSelection() {
        assertThat(client.getServiceRanking(), is(102));
    }

    @Test
    public void checkServiceSwitchingAfterShutdownAndRestart1() throws InvalidSyntaxException,
        BundleException, InterruptedException {
        assertThat(client.getServiceRanking(), is(102));
        stopRankedService(102);
        assertThat(client.getServiceRanking(), is(101));
        stopRankedService(101);
        assertThat(client.getServiceRanking(), is(100));
        startRankedService(102);
        assertThat(client.getServiceRanking(), is(102));
        startRankedService(101);
        assertThat(client.getServiceRanking(), is(102));
        stopRankedService(100);
        assertThat(client.getServiceRanking(), is(102));
    }

    @Test
    public void checkServiceSwitchingAfterShutdownAndRestart2() throws InvalidSyntaxException,
        BundleException, InterruptedException {
        assertThat(client.getServiceRanking(), is(102));
        stopRankedService(101);
        assertThat(client.getServiceRanking(), is(102));
        stopRankedService(102);
        assertThat(client.getServiceRanking(), is(100));
        startRankedService(101);
        assertThat(client.getServiceRanking(), is(101));
    }

    public void checkServiceUnavailableAfterShutdownAll() throws InvalidSyntaxException,
        BundleException, InterruptedException {
        assertThat(client.getServiceRanking(), is(102));
        stopRankedService(100);
        stopRankedService(101);
        stopRankedService(102);

        thrown.expect(ServiceLookupException.class);
        client.getServiceRanking();
    }

    private void stopRankedService(int rank) throws InvalidSyntaxException, BundleException,
        InterruptedException {
        Bundle serviceBundle = getBundle(rank);
        serviceBundle.stop();
//        while (serviceBundle.getState() != Bundle.RESOLVED) {
//            Thread.sleep(100);
//        }
    }

    private void startRankedService(int rank) throws InvalidSyntaxException, BundleException,
        InterruptedException {
        Bundle serviceBundle = getBundle(rank);
        serviceBundle.start();
//        while (serviceBundle.getState() != Bundle.ACTIVE) {
//            Thread.sleep(100);
//        }
    }

    private Bundle getBundle(int rank) {
        String sn = "org.ops4j.pax.cdi.sample7.service.impl" + rank;
        for (Bundle bundle : bc.getBundles()) {
            if (bundle.getSymbolicName().equals(sn)) {
                return bundle;
            }
        }
        return null;
    }
}
