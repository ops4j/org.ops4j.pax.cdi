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

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.sample7.api.RankedService;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.AbstractControlledTestBase.baseConfigure;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.cdiProviderBundles;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class RankingPropertyTest extends AbstractControlledTestBase {

    @Inject
    private CdiContainerFactory containerFactory;

    @Configuration
    public Option[] config() {
        return combine(
                baseConfigure(),
//                regressionDefaults(),

                workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample7-api"),
                workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample7-service-impl100"),

                paxCdiProviderAdapter(), cdiProviderBundles()
        );
    }

    @Test
    public void checkContainerFactory() {
        assertThat(containerFactory.getContainers().size(), is(1));
        CdiContainer cdiContainer = containerFactory.getContainers().iterator().next();
        assertThat(cdiContainer.getBundle().getSymbolicName(),
            is("org.ops4j.pax.cdi.sample7.service.impl100"));
    }

    @Test
    public void checkRankingProperty() {
        ServiceReference<RankedService> rankedServiceReference = bc
            .getServiceReference(RankedService.class);
        assertThat(rankedServiceReference, is(notNullValue()));
        assertThat((Integer) rankedServiceReference.getProperty(Constants.SERVICE_RANKING), is(100));
    }

}
