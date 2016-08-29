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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.api.Info;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.ApacheHttpClientConfig;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@Ignore
public class ServletTest {

    @Inject
    @Filter(timeout = 100000000)
    private CdiContainerFactory containerFactory;

    @Inject
    @Filter(timeout = 100000000)
    private CdiContainer container;

    @Inject
    @Filter(timeout = 100000000)
    private ServletContext servletContext;

    private String httpPort = System.getProperty("org.osgi.service.http.port", "8181");

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),

            // doesn't work for WABs
            // workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-samples/pax-cdi-sample1-web"),

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

    @Before
    public void before() {
        // injecting container and servletContext guarantees that initialization has completed
        // before running the tests
        assertThat(container, is(notNullValue()));
        assertThat(servletContext, is(notNullValue()));
    }

    @Test
    public void checkContainers() {
        assertThat(containerFactory.getContainers().size(), is(2));
    }

    @Test
    public void servletInjection() {
        Client client = Client.create();
        WebResource resource = client.resource(String.format("http://localhost:%s/sample1/message", httpPort));
        assertThat(resource.get(String.class), is("Message from managed bean\r\n"));
    }

    @Test
    public void servletInjectionWithRequestScope() {
        Client client = Client.create();
        WebResource resource = client.resource(String.format("http://localhost:%s/sample1/random", httpPort));
        String id1 = resource.get(String.class);
        String id2 = resource.get(String.class);
        assertThat(id1, not(id2));
    }

    @Test
    public void servletInjectionWithApplicationScope() {
        Client client = Client.create();
        WebResource resource = client.resource(String.format("http://localhost:%s/sample1/applId", httpPort));
        String id1 = resource.get(String.class);
        String id2 = resource.get(String.class);
        assertThat(id1, is(id2));
    }

    @Test
    public void servletInjectionWithSessionScope() throws InterruptedException {
        DefaultApacheHttpClientConfig config = new DefaultApacheHttpClientConfig();
        config.getProperties().put(ApacheHttpClientConfig.PROPERTY_HANDLE_COOKIES, true);
        Client client = ApacheHttpClient.create(config);
        WebResource resource = client.resource(String.format("http://localhost:%s/sample1/session", httpPort));
        String text = resource.get(String.class);
        assertThat(text, is("It worked!\n"));

        resource = client.resource(String.format("http://localhost:%s/sample1/timestamp", httpPort));
        String timestamp1 = resource.get(String.class);
        Thread.sleep(500);

        // force new session
        Client client2 = ApacheHttpClient.create(config);
        client2.resource(String.format("http://localhost:%s/sample1/session", httpPort)).get(String.class);

        WebResource resource2 = client2.resource(String.format("http://localhost:%s/sample1/timestamp", httpPort));
        String timestamp3 = resource2.get(String.class);
        assertThat(timestamp3, is(not(timestamp1)));

        String timestamp2 = resource.get(String.class);
        assertThat(timestamp1, is(timestamp2));

        String timestamp4 = resource2.get(String.class);
        assertThat(timestamp4, is(timestamp3));
    }

    @Test
    public void checkInvalidateSession() {
        Client client = Client.create();
        WebResource contextRoot = client.resource(String.format("http://localhost:%s/sample1", httpPort));
        WebResource resource1 = contextRoot.path("session");
        assertThat(resource1.get(String.class), is("It worked!\n"));

        WebResource resource2 = contextRoot.path("invalidate").queryParam("isBeanConstructed", "");
        assertThat(resource2.get(String.class), is("true"));

        WebResource resource3 = contextRoot.path("invalidate");
        assertThat(resource3.get(String.class), is(""));

        WebResource resource4 = contextRoot.path("invalidate").queryParam("isBeanDestroyed", "");
        assertThat(resource4.get(String.class), is("false"));
    }

    @Test
    public void checkOsgiServiceInjection() {
        Client client = Client.create();
        WebResource contextRoot = client.resource(String.format("http://localhost:%s/sample1", httpPort));
        WebResource resource1 = contextRoot.path("ice");
        String output = resource1.get(String.class);
        assertThat(output, containsString("Chocolate by filter"));
        assertThat(output, containsString("++++ Hazelnut"));
    }
}
