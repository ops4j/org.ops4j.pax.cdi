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
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.ops4j.pax.cdi.test.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

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

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ProducerAndClientTest {

    @Inject
    private CdiContainerFactory containerFactory;

    @Inject
    @Filter(timeout = 10000000)
    private IceCreamClient client;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),

            workspaceBundle("pax-cdi-samples/pax-cdi-sample1"),
            workspaceBundle("pax-cdi-samples/pax-cdi-sample1-client"),
            workspaceBundle("pax-cdi-extender"),
            workspaceBundle("pax-cdi-extension"),
            workspaceBundle("pax-cdi-api"),
            workspaceBundle("pax-cdi-spi"),
            workspaceBundle("pax-cdi-openwebbeans"),

            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-tracker").versionAsInProject(),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-impl").versionAsInProject(),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-spi").versionAsInProject(),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.javassist").versionAsInProject(),
            mavenBundle("org.apache.geronimo.bundles", "scannotation").versionAsInProject(),
            mavenBundle("org.apache.xbean", "xbean-finder").versionAsInProject(),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.asm").versionAsInProject(), //
            mavenBundle("org.slf4j", "jul-to-slf4j").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-validation_1.0_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jcdi_1.0_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-interceptor_1.1_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-el_2.2_spec").versionAsInProject());

    }

    @Test
    public void checkContainers() throws InterruptedException {
        assertThat(containerFactory.getProviderName(), is("org.apache.webbeans.context.WebBeansContext"));
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
        assertThat(client.getAllFlavours().size(), is(2));
        assertThat(client.getAllFlavours(), hasItems("Vanilla", "Chocolate"));
    }
}
