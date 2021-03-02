/*
 * Copyright 2015 Harald Wellmann.
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

import javax.enterprise.inject.spi.BeanManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.sample1.IceCreamService;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.ops4j.pax.swissbox.tracker.ServiceLookup;
import org.ops4j.pax.swissbox.tracker.ServiceLookupException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.cdiProviderBundles;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ExtenderLifecyleTest extends AbstractControlledTestBase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Configuration
    public Option[] config() {
        return combine(
                baseConfigure(),
//                regressionDefaults(),

                workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample1"),

                paxCdiProviderAdapter(),
                cdiProviderBundles()
        );
    }

    @Test
    public void shouldUnregisterCdiContainerOnExtenderStopped() throws BundleException {
        ServiceReference<?> ref = ServiceLookup.getServiceReference(bc,
            CdiContainer.class.getName(), 1000, null);
        assertThat(ref, is(notNullValue()));

        stopExtender();

        thrown.expect(ServiceLookupException.class);
        ServiceLookup.getServiceReference(bc, CdiContainer.class.getName(), 1000, null);
    }

    private void stopExtender() throws BundleException {
        Bundle extender = findExtender();
        extender.stop();
    }

    private Bundle findExtender() {
        Bundle extender = BundleUtils.getBundle(bc, "org.ops4j.pax.cdi.extender");
        assertThat(extender, is(notNullValue()));
        return extender;
    }

    private void startExtender() throws BundleException {
        Bundle extender = findExtender();
        extender.start();
    }

    @Test
    public void shouldUnregisterBeanManagerOnExtenderStopped() throws BundleException {
        ServiceReference<?> ref = ServiceLookup.getServiceReference(bc,
            BeanManager.class.getName(), 1000, null);
        assertThat(ref, is(notNullValue()));

        stopExtender();

        thrown.expect(ServiceLookupException.class);
        ServiceLookup.getServiceReference(bc, BeanManager.class.getName(), 1000, null);
    }

    @Test
    public void shouldUnregisterServiceComponentsOnExtenderStopped() throws BundleException {
        ServiceReference<?> ref = ServiceLookup.getServiceReference(bc,
            IceCreamService.class.getName(), 1000, null);
        assertThat(ref, is(notNullValue()));

        stopExtender();

        thrown.expect(ServiceLookupException.class);
        ServiceLookup.getServiceReference(bc, IceCreamService.class.getName(), 1000, null);
    }

    @Test
    public void shouldRegisterServiceComponentsOnExtenderRestarted() throws BundleException {
        stopExtender();
        try {
            ServiceLookup.getServiceReference(bc, IceCreamService.class.getName(), 1000, null);
        }
        catch (ServiceLookupException exc) {
            // ignore
        }
        startExtender();

        ServiceReference<?> ref = ServiceLookup.getServiceReference(bc,
            IceCreamService.class.getName(), 1000, null);
        assertThat(ref, is(notNullValue()));
    }
}
