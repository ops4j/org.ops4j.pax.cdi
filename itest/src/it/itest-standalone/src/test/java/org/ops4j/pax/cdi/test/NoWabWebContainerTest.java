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

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.sample1.client.IceCreamClient;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderJettyAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxWebBundles;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class NoWabWebContainerTest extends AbstractControlledTestBase {

    @Inject
    private CdiContainerFactory containerFactory;

    @Inject
    @Filter
    private IceCreamClient client;

    @Configuration
    public Option[] config() {
        return combine(
                baseConfigure(),
//                regressionDefaults(),

                workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample1"),
                workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample1-client"),

                paxCdiProviderAdapter(),
                paxCdiProviderJettyAdapter(),

                propagateSystemProperty("org.osgi.service.http.port"),
                paxWebBundles(),

                mavenBundle("com.sun.jersey", "jersey-core").version("1.13"),
                mavenBundle("com.sun.jersey", "jersey-client").version("1.13"),
                mavenBundle("com.sun.jersey.contribs", "jersey-apache-client").version("1.13"),
                mavenBundle("org.apache.servicemix.bundles",
                        "org.apache.servicemix.bundles.commons-httpclient", "3.1_7"),
                mavenBundle("commons-codec", "commons-codec", "1.6"),
                mavenBundle("org.slf4j", "jcl-over-slf4j", "1.6.0")
        );
    }

    @Test
    public void checkContainers() throws InterruptedException {
        assertThat(containerFactory.getContainers().size(), is(2));
        List<String> beanBundles = new ArrayList<String>();
        for (CdiContainer container : containerFactory.getContainers()) {
            beanBundles.add(container.getBundle().getSymbolicName());
        }
        assertThat(beanBundles,
            hasItems("org.ops4j.pax.cdi.sample1", "org.ops4j.pax.cdi.sample1.client"));
        assertThat(beanBundles.size(), is(2));
    }

    @Test
    public void checkBeanBundleClient() throws InterruptedException {
        assertThat(client.getFlavour(), is("Chocolate"));
    }

    @Test
    public void checkMultipleInstances() throws InterruptedException {
        assertThat(client.getAllFlavours().size(), is(4));
        assertThat(client.getAllFlavours(),
            hasItems("Vanilla", "Chocolate", "Hazelnut", "Cappuccino"));
    }
}
