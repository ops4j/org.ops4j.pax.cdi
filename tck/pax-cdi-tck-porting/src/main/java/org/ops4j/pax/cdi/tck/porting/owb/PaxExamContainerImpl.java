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
package org.ops4j.pax.cdi.tck.porting.owb;

import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;

import org.jboss.testharness.api.DeploymentException;
import org.jboss.testharness.spi.Containers;
import org.ops4j.io.StreamUtils;
import org.ops4j.io.ZipExploder;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.options.UrlProvisionOption;
import org.ops4j.pax.exam.spi.PaxExamRuntime;
import org.ops4j.pax.exam.util.PathUtils;
import org.ops4j.pax.swissbox.framework.ServiceLookup;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.google.common.io.Resources;

public class PaxExamContainerImpl implements Containers {

    private static Logger log = LoggerFactory.getLogger(PaxExamContainerImpl.class);

    private TestContainer testContainer;
    private TestContainerException deploymentException;
    private String deploymentName;

    @Override
    public boolean deploy(InputStream archive, String name) throws IOException {
        try {
            startPaxExamContainer();
            this.deploymentName = name;

            File tempDir = explodeArchive(archive);
            createManifest(name, tempDir);

            Files.copy(Resources.newInputStreamSupplier(getClass().getResource(
                "/META-INF/test-harness.properties")), new File(tempDir,
                "META-INF/jboss-test-harness.properties"));
            InputStream bundle = new JarCreator(tempDir).jar();

            log.info("test WAR in {}", tempDir);
            testContainer.install(bundle);
            
            waitForCdiContainerService();
            
            return true;
        }
        catch (TestContainerException exc) {
            deploymentException = exc;
            return false;
        }
    }

    private void waitForCdiContainerService() {
        try {
            Field field = testContainer.getClass().getDeclaredField("m_framework");
            field.setAccessible(true);
            Framework framework = (Framework) field.get(testContainer);
            BundleContext bc = framework.getBundleContext();
            ServiceLookup.getService(bc, "org.ops4j.pax.cdi.spi.CdiContainer");
        }
        catch (SecurityException exc) {
            throw new TestContainerException(exc);
        }
        catch (NoSuchFieldException exc) {
            throw new TestContainerException(exc);
        }
        catch (IllegalArgumentException exc) {
            throw new TestContainerException(exc);
        }
        catch (IllegalAccessException exc) {
            throw new TestContainerException(exc);
        }
    }

    private File explodeArchive(InputStream archive) throws IOException {
        File tempDir = Files.createTempDir();
        File zip = File.createTempFile("tck", ".zip");
        FileOutputStream os = new FileOutputStream(zip);
        StreamUtils.copyStream(archive, os, true);
        ZipExploder exploder = new ZipExploder();
        exploder.processFile(zip.getPath(), tempDir.getPath());
        return tempDir;
    }

    private void createManifest(String name, File tempDir) throws IOException {
        File manifest = new File(tempDir, "META-INF/MANIFEST.MF");
        FileOutputStream mos = new FileOutputStream(manifest);
        InputStream mis = getClass().getResourceAsStream("/probe-manifest.txt");
        StreamUtils.copyStream(mis, mos, false);
        OutputStreamWriter writer = new OutputStreamWriter(mos);
        writer.write(String.format("Web-ContextPath: /%s\n", name.substring(0, name.length() - 4)));
        writer.close();
        mis.close();
        mos.close();
    }

    @Override
    public DeploymentException getDeploymentException() {
        return new DeploymentException(deploymentName, deploymentException);
    }

    @Override
    public void undeploy(String name) throws IOException {
        testContainer.stop();
    }

    @Override
    public void setup() throws IOException {
    }

    private void startPaxExamContainer() throws IOException {
        Option[] options = getConfigurationOptions();
        ExamSystem system = PaxExamRuntime.createServerSystem(options);
        testContainer = PaxExamRuntime.createContainer(system);
        testContainer.start();
    }

    private Option[] getConfigurationOptions() {
        return options(

            bootDelegationPackage("sun.*"),
            cleanCaches(),
            frameworkStartLevel(20),
            frameworkProperty("osgi.console").value("6666"),
            frameworkProperty("osgi.debug").value("equinox-debug.properties"),

            workspaceBundle("pax-cdi-extender"),
            workspaceBundle("pax-cdi-extension"),
            workspaceBundle("pax-cdi-api"),
            workspaceBundle("pax-cdi-spi"),

            workspaceBundle("pax-cdi-web"),
            workspaceBundle("pax-cdi-openwebbeans").startLevel(2),
            workspaceBundle("pax-cdi-web-openwebbeans"),

            mavenBundle("org.ops4j.base", "ops4j-base-lang", "1.4.0"),
            mavenBundle("org.ops4j.base", "ops4j-base-spi", "1.4.0"),
            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-core", "1.5.1"),
            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-extender", "1.5.1"),
            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-framework", "1.5.1"),
            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-lifecycle", "1.5.1"),
            mavenBundle("org.ops4j.pax.swissbox", "pax-swissbox-tracker").versionAsInProject(),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-impl").versionAsInProject(),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-spi").versionAsInProject(),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-web").versionAsInProject(),

            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.javassist")
                .versionAsInProject(),
            mavenBundle("org.apache.geronimo.bundles", "scannotation").versionAsInProject(),
            mavenBundle("org.apache.xbean", "xbean-bundleutils").versionAsInProject(),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.asm")
                .versionAsInProject(), //
            mavenBundle("org.slf4j", "jul-to-slf4j").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec")
                .versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-validation_1.0_spec")
                .versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-atinject_1.0_spec")
                .versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jcdi_1.0_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-interceptor_1.1_spec")
                .versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-el_2.2_spec").versionAsInProject(),

            // Pax Web

            systemProperty("org.osgi.service.http.port").value("8181"),
            mavenBundle("org.ops4j.pax.web", "pax-web-spi").version("3.0.0.M1"),
            mavenBundle("org.ops4j.pax.web", "pax-web-api").version("3.0.0.M1"),
            mavenBundle("org.ops4j.pax.web", "pax-web-extender-war").version("3.0.0.M1")
                .startLevel(10),
            mavenBundle("org.ops4j.pax.web", "pax-web-extender-whiteboard").version("3.0.0.M1"),
            mavenBundle("org.ops4j.pax.web", "pax-web-jetty").version("3.0.0.M1"),
            mavenBundle("org.ops4j.pax.web", "pax-web-runtime").version("3.0.0.M1"),
            mavenBundle("org.ops4j.pax.web", "pax-web-jsp").version("3.0.0.M1"),
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
            mavenBundle("com.sun.jersey", "jersey-core").version("1.13"),
            mavenBundle("com.sun.jersey", "jersey-client").version("1.13"),
            mavenBundle("com.sun.jersey.contribs", "jersey-apache-client").version("1.13"),
            mavenBundle("org.apache.servicemix.bundles",
                "org.apache.servicemix.bundles.commons-httpclient", "3.1_7"),
            mavenBundle("commons-codec", "commons-codec", "1.6"),
            mavenBundle("org.slf4j", "jcl-over-slf4j", "1.6.0"),
            mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.4.0"),

            mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
            mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
            mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject());

    }

    @Override
    public void cleanup() throws IOException {
        testContainer.stop();
    }

    public static UrlProvisionOption workspaceBundle(String pathFromRoot) {
        String url = String.format("reference:file:%s/../../%s/target/classes",
            PathUtils.getBaseDir(), pathFromRoot);
        return bundle(url);
    }
}
