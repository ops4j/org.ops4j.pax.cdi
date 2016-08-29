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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.cdiProviderBundles;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.util.HashSet;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.sample5.Client;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@Ignore
public class ServiceScopeTest {

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

            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample5").startLevel(90),
            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample5-client1").startLevel(90),
            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample5-client2").startLevel(90),
            paxCdiProviderAdapter(),
            cdiProviderBundles());
    }

    @Test
    public void checkSingletonScope() {
        assertThat(client11.getSingletonScoped1().getNumber(), is(1));
        assertThat(client11.getSingletonScoped2().getNumber(), is(1));
        assertThat(client12.getSingletonScoped1().getNumber(), is(1));
        assertThat(client12.getSingletonScoped2().getNumber(), is(1));
        assertThat(client21.getSingletonScoped1().getNumber(), is(1));
        assertThat(client21.getSingletonScoped2().getNumber(), is(1));
        assertThat(client22.getSingletonScoped1().getNumber(), is(1));
        assertThat(client22.getSingletonScoped2().getNumber(), is(1));
    }

    @Test
    public void checkBundleScope() {
        assertThat(client11.getBundleScoped1().getNumber(), is(client11.getBundleScoped2().getNumber()));
        assertThat(client12.getBundleScoped1().getNumber(), is(client12.getBundleScoped2().getNumber()));
        assertThat(client11.getBundleScoped1().getNumber(), is(client12.getBundleScoped1().getNumber()));

        assertThat(client11.getBundleScoped1().getNumber(), is(not(client21.getBundleScoped1().getNumber())));

        assertThat(client21.getBundleScoped1().getNumber(), is(client21.getBundleScoped2().getNumber()));
        assertThat(client22.getBundleScoped1().getNumber(), is(client22.getBundleScoped2().getNumber()));
        assertThat(client21.getBundleScoped1().getNumber(), is(client22.getBundleScoped1().getNumber()));
    }

    @Test
    @Ignore
    public void checkPrototypeScope() {
        HashSet<Integer> numbers = new HashSet<Integer>();
        numbers.add(client11.getPrototypeScoped1().getNumber());
        numbers.add(client11.getPrototypeScoped2().getNumber());
        numbers.add(client12.getPrototypeScoped1().getNumber());
        numbers.add(client12.getPrototypeScoped2().getNumber());
        numbers.add(client21.getPrototypeScoped1().getNumber());
        numbers.add(client21.getPrototypeScoped2().getNumber());
        numbers.add(client22.getPrototypeScoped1().getNumber());
        numbers.add(client22.getPrototypeScoped2().getNumber());
        assertThat(numbers.size(), is(8));
    }
}
