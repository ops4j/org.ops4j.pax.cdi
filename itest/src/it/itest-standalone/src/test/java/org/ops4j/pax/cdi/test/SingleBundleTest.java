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

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.sample1.Chocolate;
import org.ops4j.pax.cdi.sample1.IceCreamService;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.InvalidSyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.cdiProviderBundles;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SingleBundleTest extends AbstractControlledTestBase {

    @Inject
    private CdiContainerFactory containerFactory;

    @Inject
    private CdiContainer container;

    @Inject
    private BeanManager beanManager;

    @Configuration
    public Option[] config() {
        return combine(
                baseConfigure(),
//                regressionDefaults(),

                workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample1"),
                paxCdiProviderAdapter(),
                cdiProviderBundles());
    }

    @Test
    public void checkContainerFactory() {
        assertThat(containerFactory.getContainers().size(), is(1));

        CdiContainer cdiContainer = containerFactory.getContainers().iterator().next();
        assertThat(cdiContainer.getBundle().getSymbolicName(), is("org.ops4j.pax.cdi.sample1"));
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
        assertNotNull(beanManager);
        assertThat(beanManager, is(container.getBeanManager()));
    }

    @Test
    public void vanillaIsRegisteredByClassAndInterface() throws InvalidSyntaxException {
        /*
         * org.ops4j.pax.cdi.sample1.impl is not exported, so we can reference VanillaService by
         * class name only
         */
        assertThat(bc.getServiceReference("org.ops4j.pax.cdi.sample1.impl.VanillaService"),
            is(notNullValue()));
        assertThat(bc.getServiceReferences(IceCreamService.class, "(flavour=vanilla)").isEmpty(),
            is(false));
    }

    @Test
    public void chocolateIsRegisteredByInterfaceOnly() throws InvalidSyntaxException {
        /*
         * org.ops4j.pax.cdi.sample1.impl is not exported, so we can reference ChocolateService by
         * class name only
         */
        assertThat(bc.getServiceReference("org.ops4j.pax.cdi.sample1.impl.ChocolateService"),
            is(nullValue()));
        assertThat(bc.getServiceReferences(IceCreamService.class, "(flavour=chocolate)").isEmpty(),
            is(false));
    }
}
