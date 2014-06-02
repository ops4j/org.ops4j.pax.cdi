/*
 * Copyright 2014 Harald Wellmann.
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
package org.ops4j.pax.cdi.test.jetty;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.cdiProviderBundles;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderJettyAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import javax.inject.Inject;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JettyServletTest {

    private static String httpPort = System.getProperty("http.port", "8181");

    @Inject
    private CdiContainerFactory containerFactory;

    @Inject
    private CdiContainer container;
    
    @Inject
    private ContextHandler servletContext;
    

    

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-jetty-osgi-boot"),

            mavenBundle("javax.annotation", "javax.annotation-api", "1.2"),
            provisionCoreJetty(),
            paxCdiProviderAdapter(),
            paxCdiProviderJettyAdapter(),
            cdiProviderBundles(),
            httpServiceJetty(),
            jspDependencies(),

            bootDelegationPackages("org.xml.sax", "org.xml.*", "org.w3c.*", "javax.xml.*",
                "javax.activation.*", "com.sun.org.apache.xpath.internal.jaxp"),
            systemPackages("com.sun.org.apache.xalan.internal.res",
                "com.sun.org.apache.xml.internal.utils", "com.sun.org.apache.xml.internal.utils",
                "com.sun.org.apache.xpath.internal", "com.sun.org.apache.xpath.internal.jaxp",
                "com.sun.org.apache.xpath.internal.objects",
                "org.eclipse.jetty.webapp"),
                

                mavenBundle("org.apache.felix", "org.apache.felix.eventadmin", "1.3.2"),
                
                

            mavenBundle("org.glassfish", "javax.el", "3.0.0"),
            mavenBundle("javax.enterprise", "cdi-api", "1.2"),
            mavenBundle("javax.interceptor", "javax.interceptor-api", "1.2"),
            mavenBundle("javax.validation", "validation-api", "1.1.0.Final"),

            //mavenBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample1-web", Info.getPaxCdiVersion()),
            bundle("file:target/pax-cdi-sample1-web.jar"),
            
            mavenBundle("com.sun.jersey", "jersey-core").version("1.13"),
            mavenBundle("com.sun.jersey", "jersey-client").version("1.13"),
            mavenBundle("com.sun.jersey.contribs", "jersey-apache-client").version("1.13"),
            mavenBundle("org.apache.servicemix.bundles",
                "org.apache.servicemix.bundles.commons-httpclient", "3.1_7"),
            mavenBundle("commons-codec", "commons-codec", "1.6")
                
            );

    }

    public static Option provisionCoreJetty() {
        return composite(
            systemProperty("jetty.port").value(httpPort), 
            systemProperty("jetty.home.bundle").value("org.eclipse.jetty.osgi.boot"), 
            coreJettyDependencies());
    }

    public static Option coreJettyDependencies() {

        return composite(

            mavenBundle("javax.servlet", "javax.servlet-api").versionAsInProject(),
            mavenBundle("javax.annotation", "javax.annotation-api").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec").version("1.1.1"),
            mavenBundle("org.eclipse.jetty.orbit", "javax.mail.glassfish").version(
                "1.4.1.v201005082020"),

            mavenBundle("org.eclipse.jetty.toolchain", "jetty-schemas").versionAsInProject(),

            mavenBundle("org.eclipse.jetty", "jetty-deploy").versionAsInProject(),
            mavenBundle("org.eclipse.jetty", "jetty-server").versionAsInProject(),
            mavenBundle("org.eclipse.jetty", "jetty-servlet").versionAsInProject(),
            mavenBundle("org.eclipse.jetty", "jetty-util").versionAsInProject(),
            mavenBundle("org.eclipse.jetty", "jetty-http").versionAsInProject(),
            mavenBundle("org.eclipse.jetty", "jetty-xml").versionAsInProject(),
            mavenBundle("org.eclipse.jetty", "jetty-webapp").versionAsInProject(),
            mavenBundle("org.eclipse.jetty", "jetty-io").versionAsInProject(),
            mavenBundle("org.eclipse.jetty", "jetty-continuation").versionAsInProject(),
            mavenBundle("org.eclipse.jetty", "jetty-security").versionAsInProject(),
            mavenBundle("org.eclipse.jetty", "jetty-servlets").versionAsInProject(),
            mavenBundle("org.eclipse.jetty", "jetty-client").versionAsInProject(),
            mavenBundle("org.eclipse.jetty", "jetty-jndi").versionAsInProject(),
            mavenBundle("org.eclipse.jetty", "jetty-plus").versionAsInProject());
            
    }

    public static Option httpServiceJetty() {
        return composite(
//            mavenBundle("org.eclipse.jetty.osgi", "jetty-httpservice")
//            .versionAsInProject(), 
            bundle("file:target/jetty-httpservice.jar"),
            mavenBundle("org.eclipse.equinox.http", "servlet")
            .versionAsInProject());
    }

    public static Option jspDependencies() {
        return composite(
            mavenBundle("javax.servlet.jsp", "javax.servlet.jsp-api").versionAsInProject(),
            mavenBundle("org.eclipse.jetty.orbit", "javax.servlet.jsp.jstl").versionAsInProject(),
            mavenBundle("org.glassfish.web", "javax.servlet.jsp.jstl").versionAsInProject(),
            mavenBundle("org.glassfish", "javax.el").versionAsInProject(),
            mavenBundle("org.eclipse.jetty.orbit", "org.eclipse.jdt.core").versionAsInProject(),
            mavenBundle("org.eclipse.jetty.toolchain", "jetty-jsp-fragment").versionAsInProject()
                .noStart(), 
            mavenBundle("org.eclipse.jetty.osgi", "jetty-osgi-boot-jsp")
                .versionAsInProject().noStart());

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
        assertThat(containerFactory.getContainers().size(), is(1));
    }

    @Test
    public void servletInjection() {
        Client client = Client.create();
        WebResource resource = client.resource(String.format("http://localhost:%s/sample1/message", httpPort));
        assertThat(resource.get(String.class), is("Message from managed bean\r\n"));
    }

}
