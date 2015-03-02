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

import java.util.Collection;

import javax.inject.Inject;

import org.eclipse.osgi.framework.internal.core.Constants;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.sample7.api.RankedService;
import org.ops4j.pax.cdi.sample7.api.RankedServiceClient;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class DynamicServiceSwitchingTest {

	@Inject
	private CdiContainerFactory	containerFactory;

	@Inject
	private BundleContext		bc;

	@Inject
	private RankedServiceClient	sut;				// System under test

	@Configuration
	public Option[] config() {
		return options(
				regressionDefaults(),
				workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample7-api"),
				workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample7-service-impl100"),
				workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample7-service-impl101"),
				workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample7-service-impl102"),
				workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample7-client-dynamic"),
				paxCdiProviderAdapter(),
				cdiProviderBundles());
	}

	@Test
	public void checkContainerFactory() {
		System.out.println("Containers: " + this.containerFactory.getContainers().size());
		assertThat(this.containerFactory.getContainers().size(), is(4));
	}

	@Test
	public void checkInitialServiceSelection() {
		System.out.println("sut.getServiceRanking(): " + this.sut.getServiceRanking());
		assertThat(this.sut.getServiceRanking(), is(102));
	}
	
	@Test
	public void checkServiceSwitchingAfterShutdown102() throws InvalidSyntaxException, BundleException {
		ServiceReference<RankedService> service102Reference = getRankedServiceReference(102);
		Bundle service102Bundle = service102Reference.getBundle();
		service102Bundle.stop();
		while (service102Bundle.getState() != Bundle.RESOLVED) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// nop
			}
		}
		System.out.println("sut.getServiceRanking(): " + this.sut.getServiceRanking());
		assertThat(this.sut.getServiceRanking(), is(101));
	}

	private ServiceReference<RankedService> getRankedServiceReference(int ranking) throws InvalidSyntaxException {
		Collection<ServiceReference<RankedService>> rankedServiceReferences = this.bc.getServiceReferences(
				RankedService.class,
				"(" + Constants.SERVICE_RANKING + "=" + ranking + ")");
		if (rankedServiceReferences.size() == 1) {
			return rankedServiceReferences.iterator().next();
		}
		Assert.fail("Not exact one RankedService with rank " + ranking);
		return null;
	}
}
