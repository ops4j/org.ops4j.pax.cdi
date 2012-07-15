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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import org.apache.webbeans.context.WebBeansContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.sample1.Chocolate;
import org.ops4j.pax.cdi.sample1.ChocolateService;
import org.ops4j.pax.cdi.sample1.IceCreamService;
import org.ops4j.pax.cdi.sample1.VanillaService;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SingleBundleTest {

    @Inject
    private CdiContainerFactory containerFactory;

    @Inject
    private CdiContainer container;
    
    @Inject
    private BundleContext bc;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),

            workspaceBundle("pax-cdi-samples/pax-cdi-sample1"),
            workspaceBundle("pax-cdi-extender"),
            workspaceBundle("pax-cdi-extension"),
            workspaceBundle("pax-cdi-api"),
            workspaceBundle("pax-cdi-spi"),
            workspaceBundle("pax-cdi-openwebbeans"),

            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-tracker", "1.5.1"),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-impl", "1.1.4"),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-spi", "1.1.4"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.javassist",
                "3.12.1.GA_3"),
            mavenBundle("org.apache.geronimo.bundles", "scannotation", "1.0.2_1"),
            mavenBundle("org.apache.xbean", "xbean-finder", "3.7"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.asm",
                "3.3.1_1"), mavenBundle("org.slf4j", "jul-to-slf4j", "1.6.4"),
            mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec", "1.0"),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec", "1.1.1"),
            mavenBundle("org.apache.geronimo.specs", "geronimo-validation_1.0_spec", "1.1"),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jcdi_1.0_spec", "1.0"),
            mavenBundle("org.apache.geronimo.specs", "geronimo-interceptor_1.1_spec", "1.0"),
            mavenBundle("org.apache.geronimo.specs", "geronimo-el_2.2_spec", "1.0"));

    }

    @Test
    public void checkContainerFactory() {
        assertThat(containerFactory.getProviderName(), is(WebBeansContext.class.getName()));
        assertThat(containerFactory.getContainers().size(), is(1));

        CdiContainer container = containerFactory.getContainers().iterator().next();
        assertThat(container.getBundle().getSymbolicName(), is("org.ops4j.pax.cdi.sample1"));
    }

    @SuppressWarnings("serial")
    @Test
    public void checkContainerInstance() {
        Instance<Object> instance = container.getInstance();
        assertThat(instance, is(notNullValue()));
        Instance<IceCreamService> iceCreamInstance = instance.select(IceCreamService.class);
        assertThat(iceCreamInstance, is(notNullValue()));

        Instance<IceCreamService> chocolateInstance = iceCreamInstance
            .select(new AnnotationLiteral<Chocolate>() {
            });
        assertThat(chocolateInstance.isAmbiguous(), is(false));

        IceCreamService chocolate = chocolateInstance.get();
        assertThat(chocolate, is(notNullValue()));
        assertThat(chocolate.getFlavour(), is("Chocolate"));

        Event<Object> event = container.getEvent();
        assertThat(event, is(notNullValue()));
    }

    @Test
    public void checkBeanManager() {
        assertNotNull(container.getBeanManager());
    }
    
    @Test
    public void vanillaIsRegisteredByClassAndInterface() throws InvalidSyntaxException {
        assertThat(bc.getServiceReference(VanillaService.class), is(notNullValue()));
        assertThat(bc.getServiceReferences(IceCreamService.class, "(flavour=vanilla)").isEmpty(), is(false));
    }

    @Test
    public void chocolateIsRegisteredByInterfaceOnly() throws InvalidSyntaxException {
        assertThat(bc.getServiceReference(ChocolateService.class), is(nullValue()));
        assertThat(bc.getServiceReferences(IceCreamService.class, "(flavour=chocolate)").isEmpty(), is(false));
    }
}
