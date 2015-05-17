/*
 * Copyright 2013 Harald Wellmann.
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
package org.ops4j.pax.cdi.arquillian;

import static org.ops4j.pax.cdi.test.support.TestConfiguration.cdiProviderBundles;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderUndertowAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxWebUndertowBundles;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.codehaus.plexus.util.IOUtil;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.spi.PaxExamRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaxCdiArquillianContainer implements DeployableContainer<PaxCdiConfiguration> {

    private static Logger log = LoggerFactory.getLogger(PaxCdiArquillianContainer.class);

    private static String paxCdiRoot;

    private TestContainer testContainer;

    @Override
    public Class<PaxCdiConfiguration> getConfigurationClass() {
        return PaxCdiConfiguration.class;
    }

    @Override
    public void setup(PaxCdiConfiguration configuration) {
    }

    @Override
    public void start() throws LifecycleException {
        Option[] options = getConfigurationOptions();
        try {
            ExamSystem system = PaxExamRuntime.createServerSystem(options);
            testContainer = PaxExamRuntime.createContainer(system);
            testContainer.start();
        }
        catch (IOException | TestContainerException | SecurityException | IllegalArgumentException exc) {
            log.error("error starting Pax Exam container", exc);
        }
    }

    @Override
    public void stop() throws LifecycleException {
        testContainer.stop();
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("pax-cdi");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        archive.delete("WEB-INF/web.xml");

        WebArchive war = archive.as(WebArchive.class);
        Asset manifest = buildManifest(war);
        war.setManifest(manifest);

        war.setWebXML(new File(getPaxCdiRoot(),
            "tck/pax-cdi-arquillian/src/main/resources/probe-web.xml"));
        InputStream is = archive.as(ZipExporter.class).exportAsInputStream();
        testContainer.installProbe(is);
        // FIXME implement proper synchronization
        try {
            Thread.sleep(2000);
        }
        catch (InterruptedException exc) {
            log.error("error starting Pax Exam container", exc);
        }

        ProtocolMetaData metadata = new ProtocolMetaData();
        HTTPContext context = new HTTPContext("localhost", 8181);
        context.add(new Servlet("ArquillianServletRunner", "/probe"));
        metadata.addContext(context);
        return metadata;
    }

    private Asset buildManifest(WebArchive war) {
        String manifest = null;
        try {
            manifest = IOUtil.toString(getClass().getResourceAsStream("/probe-manifest.txt"));
        }
        catch (IOException exc) {
            throw new Ops4jException(exc);
        }

        StringBuilder buffer = new StringBuilder(manifest);
        buffer.append("Bundle-ClassPath: WEB-INF/classes");
        for (ArchivePath path : war.getContent().keySet()) {
            String jar = path.get().substring(1);
            if (jar.startsWith("WEB-INF/lib/") && (jar.endsWith(".jar") || jar.endsWith(".zip"))) {
                buffer.append(", \n ");
                buffer.append(jar);
            }
        }
        buffer.append("\n");
        return new StringAsset(buffer.toString());
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        testContainer.uninstallProbe();
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException();
    }

    private Option[] getConfigurationOptions() {
        Properties props = new Properties();
        try {
            props.load(PaxCdiArquillianContainer.class.getResourceAsStream("/systemPackages.properties"));
        }
        catch (IOException exc) {
            throw new Ops4jException(exc);
        }


        return options(
            bootDelegationPackage("sun.*"),

            cleanCaches(),
            frameworkStartLevel(20),
            frameworkProperty("osgi.console").value("6666"),
            frameworkProperty("osgi.debug").value("equinox-debug.properties"),

            // do not treat javax.annotation as system package
            frameworkProperty("org.osgi.framework.system.packages").value(props.get("org.osgi.framework.system.packages")),


            mavenBundle("org.ops4j.base", "ops4j-base", "1.5.0"),
            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-core", "1.8.1"),
            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-extender", "1.8.1"),
            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-framework", "1.8.1"),
            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-lifecycle", "1.8.1"),
            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-tracker", "1.8.1"),
            mavenBundle("org.apache.felix", "org.apache.felix.scr", "1.8.2"),
            mavenBundle("org.apache.felix", "org.apache.felix.jaas", "0.0.2"),
            mavenBundle("org.osgi", "org.osgi.compendium", "4.3.1"),
            mavenBundle("org.apache.xbean", "xbean-bundleutils", "3.18"),
            mavenBundle("org.apache.xbean", "xbean-asm5-shaded", "3.18"), //
            mavenBundle("org.apache.xbean", "xbean-finder-shaded", "3.18"), //
            mavenBundle("org.apache.geronimo.specs", "geronimo-atinject_1.0_spec").versionAsInProject(),

            cdiProviderBundles(),
            paxCdiProviderAdapter(),
            paxCdiProviderUndertowAdapter(),

            systemProperty("org.osgi.service.http.port").value("8181"),
            paxWebUndertowBundles(),


            mavenBundle("commons-beanutils", "commons-beanutils", "1.8.3"),
            mavenBundle("commons-collections", "commons-collections", "3.2.1"),
            mavenBundle("org.apache.servicemix.bundles",
                "org.apache.servicemix.bundles.commons-digester", "1.8_4"),

            mavenBundle("com.sun.jersey", "jersey-core").version("1.13"),
            mavenBundle("com.sun.jersey", "jersey-client").version("1.13"),
            mavenBundle("com.sun.jersey.contribs", "jersey-apache-client").version("1.13"),
            mavenBundle("org.apache.servicemix.bundles",
                "org.apache.servicemix.bundles.commons-httpclient", "3.1_7"),
            mavenBundle("commons-codec", "commons-codec", "1.6"),
            mavenBundle("org.slf4j", "jcl-over-slf4j", "1.6.0"),

            mavenBundle("org.slf4j", "slf4j-api", "1.7.2"),
            mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
            mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),

            // options required for Forked Container, having no effect in Native Container
            vmOption("-ea"),
            systemProperty("logback.configurationFile").value(getPaxCdiRoot() + "/tck/pax-cdi-arquillian/src/test/resources/logback.xml"));

    }

    public static String getPaxCdiRoot() {
        if (paxCdiRoot == null) {
            paxCdiRoot = System.getProperty("pax.cdi.root");
            if (paxCdiRoot == null) {
                Properties props = new Properties();
                try {
                    props.load(PaxCdiArquillianContainer.class
                        .getResourceAsStream("/org.ops4j.pax.cdi.properties"));
                    paxCdiRoot = props.getProperty("pax.cdi.root");
                }
                catch (IOException exc) {
                    throw new Ops4jException(exc);
                }
            }
        }
        return paxCdiRoot;
    }
}
