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
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import javax.inject.Inject;

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
import org.osgi.framework.BundleContext;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.ApacheHttpClientConfig;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ServletTest {

    @Inject @Filter (timeout = 20000000)
    private BundleContext bc;

    @Inject @Filter (timeout = 20000000)
    private CdiContainerFactory containerFactory;

    @Inject @Filter (timeout = 20000000)
    private CdiContainer container;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),

            // doesn't work for WABs
            // workspaceBundle("pax-cdi-samples/pax-cdi-sample1-web"),

            mavenBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample1-web", Info.getPaxCdiVersion()),
            workspaceBundle("pax-cdi-extender"),
            workspaceBundle("pax-cdi-extension"),
            workspaceBundle("pax-cdi-api"),
            workspaceBundle("pax-cdi-spi"),
            workspaceBundle("pax-cdi-web"),
            workspaceBundle("pax-cdi-openwebbeans").startLevel(2),
            workspaceBundle("pax-cdi-web-openwebbeans"),

            mavenBundle("org.apache.openwebbeans", "openwebbeans-impl").versionAsInProject(),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-spi").versionAsInProject(),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-web").versionAsInProject(),

            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.javassist")
                .versionAsInProject(),
            mavenBundle("org.apache.geronimo.bundles", "scannotation").versionAsInProject(),
            mavenBundle("org.apache.xbean", "xbean-bundleutils").versionAsInProject(),
            mavenBundle("org.apache.xbean", "xbean-finder").versionAsInProject(),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.asm")
                .versionAsInProject(), //
            mavenBundle("org.slf4j", "jul-to-slf4j").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec")
                .versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-validation_1.0_spec")
                .versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jcdi_1.0_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-interceptor_1.1_spec")
                .versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-el_2.2_spec").versionAsInProject(),

            // Pax Web

            systemProperty("org.osgi.service.http.port").value("8181"),
            mavenBundle("org.ops4j.pax.web", "pax-web-spi").version(Info.getPaxWebVersion()),
            mavenBundle("org.ops4j.pax.web", "pax-web-api").version(Info.getPaxWebVersion()),
            mavenBundle("org.ops4j.pax.web", "pax-web-extender-war").version(Info.getPaxWebVersion())
                .startLevel(10),
            mavenBundle("org.ops4j.pax.web", "pax-web-extender-whiteboard").version(
                Info.getPaxWebVersion()),
            mavenBundle("org.ops4j.pax.web", "pax-web-jetty").version(Info.getPaxWebVersion()),
            mavenBundle("org.ops4j.pax.web", "pax-web-runtime").version(Info.getPaxWebVersion()),
            mavenBundle("org.ops4j.pax.web", "pax-web-jsp").version(Info.getPaxWebVersion()),
            mavenBundle("org.eclipse.jdt.core.compiler", "ecj").version("3.5.1"),
            mavenBundle("org.eclipse.jetty", "jetty-util").version("8.1.4.v20120524"),
            mavenBundle("org.eclipse.jetty", "jetty-io").version("8.1.4.v20120524"),
            mavenBundle("org.eclipse.jetty", "jetty-http").version("8.1.4.v20120524"),
            mavenBundle("org.eclipse.jetty", "jetty-continuation").version("8.1.4.v20120524"),
            mavenBundle("org.eclipse.jetty", "jetty-server").version("8.1.4.v20120524"),
            mavenBundle("org.eclipse.jetty", "jetty-security").version("8.1.4.v20120524"),
            mavenBundle("org.eclipse.jetty", "jetty-xml").version("8.1.4.v20120524"),
            mavenBundle("org.eclipse.jetty", "jetty-servlet").version("8.1.4.v20120524"),
            mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec").version("1.0"),
            mavenBundle("org.osgi", "org.osgi.compendium").version("4.3.1"),
            mavenBundle("com.sun.jersey", "jersey-core").version("1.13"),
            mavenBundle("com.sun.jersey", "jersey-client").version("1.13"),
            mavenBundle("com.sun.jersey.contribs", "jersey-apache-client").version("1.13"),
            mavenBundle("org.apache.servicemix.bundles",
                "org.apache.servicemix.bundles.commons-httpclient", "3.1_7"),
            mavenBundle("commons-codec", "commons-codec", "1.6"),
            mavenBundle("org.slf4j", "jcl-over-slf4j", "1.6.0"));

    }

    @Test
    public void checkContainers() {
        assertThat(containerFactory.getContainers().size(), is(1));
    }

    @Test
    public void servletInjection() {
        Client client = Client.create();
        WebResource resource = client.resource("http://localhost:8181/sample1/message");
        assertThat(resource.get(String.class), is("Message from managed bean\r\n"));
    }

    @Test
    public void servletInjectionWithRequestScope() {
        Client client = Client.create();
        WebResource resource = client.resource("http://localhost:8181/sample1/random");
        String id1 = resource.get(String.class);
        String id2 = resource.get(String.class);
        assertThat(id1, not(id2));
    }

    @Test
    public void servletInjectionWithApplicationScope() {
        Client client = Client.create();
        WebResource resource = client.resource("http://localhost:8181/sample1/applId");
        String id1 = resource.get(String.class);
        String id2 = resource.get(String.class);
        assertThat(id1, is(id2));
    }

    @Test
    public void servletInjectionWithSessionScope() throws InterruptedException {
        DefaultApacheHttpClientConfig config = new DefaultApacheHttpClientConfig();
        config.getProperties().put(ApacheHttpClientConfig.PROPERTY_HANDLE_COOKIES, true);
        Client client = ApacheHttpClient.create(config);
        WebResource resource = client.resource("http://localhost:8181/sample1/session");
        String text = resource.get(String.class);
        assertThat(text, is("It worked!\n"));

        resource = client.resource("http://localhost:8181/sample1/timestamp");
        String timestamp1 = resource.get(String.class);

        Client client2 = ApacheHttpClient.create(config);
        WebResource resource2 = client2.resource("http://localhost:8181/sample1/timestamp");
        String timestamp3 = resource2.get(String.class);
        assertThat(timestamp3, is(not(timestamp1)));

        String timestamp2 = resource.get(String.class);
        assertThat(timestamp1, is(timestamp2));

        String timestamp4 = resource2.get(String.class);
        assertThat(timestamp4, is(timestamp3));

    }
}
