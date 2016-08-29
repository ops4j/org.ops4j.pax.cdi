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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.cdiProviderBundles;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderWebAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxWebBundles;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.api.Info;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.ops4j.pax.swissbox.tracker.ServiceLookup;
import org.ops4j.pax.swissbox.tracker.ServiceLookupException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@Ignore
public class WebBeanBundleLifecycleTest {

    @Inject
    private BundleContext bc;

    private String httpPort = System.getProperty("org.osgi.service.http.port", "8181");

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),

            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample1"),
            mavenBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample1-web", Info.getPaxCdiVersion()),

            cdiProviderBundles(),
            paxCdiProviderAdapter(),
            paxCdiProviderWebAdapter(),

            // Pax Web

            systemProperty("org.osgi.service.http.port").value(httpPort),
            paxWebBundles(),

            mavenBundle("com.sun.jersey", "jersey-core").version("1.13"),
            mavenBundle("com.sun.jersey", "jersey-client").version("1.13"),
            mavenBundle("com.sun.jersey.contribs", "jersey-apache-client").version("1.13"),
            mavenBundle("org.apache.servicemix.bundles",
                "org.apache.servicemix.bundles.commons-httpclient", "3.1_7"),
            mavenBundle("commons-codec", "commons-codec", "1.6"),
            mavenBundle("org.slf4j", "jcl-over-slf4j", "1.6.0"));

    }

    @Test
    public void shouldRestartWebBeanBundle() throws BundleException {
        ServiceLookup.getService(bc, ServletContext.class);
        ServiceLookup.getService(bc, CdiContainer.class);
        Client client = Client.create();
        WebResource resource = client.resource(
            String.format("http://localhost:%s/sample1/message", httpPort));
        assertThat(resource.get(String.class), is("Message from managed bean\r\n"));

        Bundle webBundle = BundleUtils.getBundle(bc, "pax-cdi-sample1-web");
        assertThat(webBundle, is(notNullValue()));
        webBundle.stop();
        try {
            ServiceLookup.getService(bc, ServletContext.class, 1000);
            fail("should not find ServletContext");
        }
        catch (ServiceLookupException exc) {
            // ignore
        }
        webBundle.start();
        ServiceLookup.getService(bc, ServletContext.class);
        ServiceLookup.getService(bc, CdiContainer.class);
        assertThat(resource.get(String.class), is("Message from managed bean\r\n"));
    }
}
