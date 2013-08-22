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
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.weld.TestConfiguration.paxWebBundles;
import static org.ops4j.pax.cdi.test.weld.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.weld.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.api.Info;
import org.ops4j.pax.cdi.sample1.client.IceCreamClient;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 * Tests injection of a managed bean into a servlet deployed in a WAB. The managed bean is
 * defined in the same WAB.
 * 
 * @author Harald Wellmann
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ServletTest {

    @Inject
    private CdiContainerFactory containerFactory;

    @Inject
    private IceCreamClient iceCreamClient;
    
    @Inject
    private ServletContext servletContext;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),


            workspaceBundle("pax-cdi-samples/pax-cdi-sample1"),
            workspaceBundle("pax-cdi-samples/pax-cdi-sample1-client"),
            
            // doesn't work for WABs
            //workspaceBundle("pax-cdi-samples/pax-cdi-sample1-web"),
            mavenBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample1-web", Info.getPaxCdiVersion()),
            
            workspaceBundle("pax-cdi-extender"),
            workspaceBundle("pax-cdi-extension"),
            workspaceBundle("pax-cdi-api"),
            workspaceBundle("pax-cdi-spi"),
            workspaceBundle("pax-cdi-web"),
            workspaceBundle("pax-cdi-web-weld"),
            workspaceBundle("pax-cdi-weld"),

            mavenBundle("org.slf4j", "slf4j-ext", "1.6.4"),
            mavenBundle("ch.qos.cal10n", "cal10n-api", "0.7.4"),
            mavenBundle("org.apache.xbean", "xbean-bundleutils").versionAsInProject(),
            mavenBundle("org.apache.xbean", "xbean-finder").versionAsInProject(),
            mavenBundle("javax.el", "javax.el-api").version("3.0.0"),
            mavenBundle("org.apache.geronimo.specs", "geronimo-interceptor_1.1_spec")
                .versionAsInProject(),
            
            // also in pax-web-jsp:    
            // mavenBundle("org.apache.geronimo.specs", "geronimo-el_2.2_spec").versionAsInProject(),
            
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.asm")
                .versionAsInProject(), //
            mavenBundle("org.jboss.weld", "weld-osgi-bundle").versionAsInProject(),

            // Pax Web

            systemProperty("org.osgi.service.http.port").value("8181"),
            paxWebBundles(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec").version("1.0"),
            mavenBundle("com.sun.jersey", "jersey-core").version("1.13"),
            mavenBundle("com.sun.jersey", "jersey-client").version("1.13"),
            mavenBundle("org.osgi", "org.osgi.compendium", "4.3.0")
        );

    }

    @Test
    public void checkContainers() {
        assertThat(containerFactory.getContainers().size(), is(3));
        Client client = Client.create();
        WebResource resource = client.resource("http://localhost:8181/sample1/message");
        assertThat(resource.get(String.class), is("Message from managed bean\r\n"));
    }

    @Test
    public void checkInvalidateSession() {
        Client client = Client.create();
        WebResource resource1 = client.resource("http://localhost:8181/sample1/session");
        assertThat(resource1.get(String.class), is("It worked!\n"));
        
        WebResource resource2 = client.resource("http://localhost:8181/sample1/invalidate?isBeanConstructed");
        assertThat(resource2.get(String.class), is("true"));

        WebResource resource3 = client.resource("http://localhost:8181/sample1/invalidate");
        assertThat(resource3.get(String.class), is(""));

        WebResource resource4 = client.resource("http://localhost:8181/sample1/invalidate?isBeanDestroyed");
        assertThat(resource4.get(String.class), is("false"));
    }    
}
