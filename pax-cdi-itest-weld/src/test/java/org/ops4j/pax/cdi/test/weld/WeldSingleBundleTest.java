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
package org.ops4j.pax.cdi.test.weld;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.weld.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.weld.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.sample1.Chocolate;
import org.ops4j.pax.cdi.sample1.IceCreamService;
import org.ops4j.pax.cdi.sample1.impl.ChocolateService;
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
public class WeldSingleBundleTest {

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
            workspaceBundle("pax-cdi-weld"),

            mavenBundle("org.slf4j", "slf4j-ext", "1.6.4"),
            mavenBundle("ch.qos.cal10n", "cal10n-api", "0.7.4"),
            //mavenBundle("com.googlecode.guava-osgi", "guava-osgi", "11.0.1"),
            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-tracker").versionAsInProject(),
            mavenBundle("org.apache.xbean", "xbean-finder").versionAsInProject(),
            //mavenBundle("org.apache.geronimo.specs", "geronimo-jcdi_1.0_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-interceptor_1.1_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-el_2.2_spec").versionAsInProject(),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.asm").versionAsInProject(), //
//            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.javassist").versionAsInProject(),
            mavenBundle("org.jboss.weld", "weld-osgi-bundle", "1.2.0-SNAPSHOT").startLevel(3)

            //wrappedBundle(mavenBundle("org.jboss.weld.se", "weld-se-core", "1.1.8.Final")).instructions("Fragment-Host=org.jboss.weld.osgi-bundle").noStart(),
//            wrappedBundle(mavenBundle("org.jboss.weld", "weld-core", "1.1.8.Final")).bundleVersion("1.1.8").instructions("Export-Package=*;version=1.1.8", "Import-Package=org.jboss.weld.context;resolution:=mandatory,javax.*;resolution:=optional,*"),
//            wrappedBundle(mavenBundle("org.jboss.weld", "weld-spi", "1.1.Final")).bundleVersion("1.1.8").instructions("Export-Package=*;version=1.1.8"),
//            wrappedBundle(mavenBundle("org.jboss.weld", "weld-api", "1.1.Final")).bundleVersion("1.1.8").instructions("Export-Package=*;version=1.1.8")
        );
    }

    //@Test
    public void checkContainerFactory() throws InterruptedException {
        assertThat(containerFactory.getProviderName(), is("org.ops4j.pax.cdi.weld.impl.Weld"));
        assertThat(containerFactory.getContainers().size(), is(1));

        CdiContainer container = containerFactory.getContainers().iterator().next();
        assertThat(container.getBundle().getSymbolicName(), is("org.ops4j.pax.cdi.sample1"));
        Thread.sleep(2000);
}

    @SuppressWarnings("serial")
    @Test
    public void checkContainerInstance() throws InterruptedException {
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
        assertThat(bc.getServiceReference("org.ops4j.pax.cdi.sample1.impl.VanillaService"), is(notNullValue()));
        assertThat(bc.getServiceReferences(IceCreamService.class, "(flavour=vanilla)").isEmpty(), is(false));
    }

    @Test
    public void chocolateIsRegisteredByInterfaceOnly() throws InvalidSyntaxException {
        assertThat(bc.getServiceReference("org.ops4j.pax.cdi.sample1.impl.ChocolateService"), is(nullValue()));
        assertThat(bc.getServiceReferences(IceCreamService.class, "(flavour=chocolate)").isEmpty(), is(false));
    }
    
}
