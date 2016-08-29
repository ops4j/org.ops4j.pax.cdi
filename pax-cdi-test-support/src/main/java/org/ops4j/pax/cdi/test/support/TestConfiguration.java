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
package org.ops4j.pax.cdi.test.support;

import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.ServiceLoader;

import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.cdi.api.Info;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.ProvisionOption;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * Reusable composite options for Pax CDI integration tests with Pax Exam.
 *
 * @author Harald Wellmann
 */
public class TestConfiguration {

    private static final String JETTY_VERSION = "9.3.11.v20160721";
    private static volatile String paxCdiRoot;

    private static boolean consoleEnabled = Boolean.getBoolean("org.ops4j.pax.cdi.console");

    private TestConfiguration() {
    }

    public static Option regressionDefaults() {
        Properties props = new Properties();
        try {
            props.load(TestConfiguration.class.getResourceAsStream("/systemPackages.properties"));
        }
        catch (IOException exc) {
            throw new Ops4jException(exc);
        }


        return composite(

            cleanCaches(),
            frameworkStartLevel(20),

            // add SLF4J and logback bundles
            mavenBundle("org.slf4j", "slf4j-api").versionAsInProject().startLevel(
                START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("ch.qos.logback", "logback-core").versionAsInProject().startLevel(
                START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject().startLevel(
                START_LEVEL_SYSTEM_BUNDLES),

            mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.6.0"),
            mavenBundle("org.apache.felix", "org.apache.felix.scr", "1.6.2"),
            mavenBundle("org.apache.xbean", "xbean-asm5-shaded", "4.4"), //
            mavenBundle("org.apache.xbean", "xbean-finder-shaded", "4.4"), //

            // Set logback configuration via system property.
            // This way, both the driver and the container use the same configuration
            systemProperty("logback.configurationFile").value(
                "file:" + PathUtils.getBaseDir() + "/src/test/resources/logback.xml"),


            when(consoleEnabled).useOptions(
                mavenBundle("org.apache.felix", "org.apache.felix.shell.remote", "1.1.2").startLevel(2),
                mavenBundle("org.apache.felix", "org.apache.felix.gogo.command", "0.14.0").startLevel(2),
                mavenBundle("org.apache.felix", "org.apache.felix.gogo.runtime", "0.12.1").startLevel(2),
                mavenBundle("org.apache.felix", "org.apache.felix.gogo.shell", "0.10.0").startLevel(2)),

            when(consoleEnabled && isEquinox()).useOptions(
                //frameworkProperty("osgi.console").value("6666"),
                frameworkProperty("eclipse.consoleLog").value("true"),
                frameworkProperty("osgi.console.enable.builtin").value("true"),
                bundle("file:target/org.eclipse.equinox.console.jar").startLevel(2)),


            // do not treat javax.annotation as system package
            frameworkProperty("org.osgi.framework.system.packages").value(props.get("org.osgi.framework.system.packages")),

            systemTimeout(2000000),
            junitBundles());
    }

    public static Option cdiProviderBundles() {
        return composite(
            cdiProviderSpecificBundles(),

            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-extender"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-extension"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-api"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-spi"));
    }

    public static Option cdiProviderSpecificBundles() {
        switch (getCdiProvider()) {

            case OWB1:
                return openWebBeans16Bundles();

            case WELD2:
                return weld2Bundles();

            default:
                throw new IllegalArgumentException("pax.cdi.provider unknown or null");
        }
    }

    public static Option paxCdiProviderAdapter() {
        switch (getCdiProvider()) {

            case OWB1:
                return workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-openwebbeans");

            case WELD2:
                return workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-weld");

            default:
                throw new IllegalArgumentException("pax.cdi.provider unknown or null");
        }
    }

    public static Option paxCdiProviderWebAdapter() {
        switch (getCdiProvider()) {

            case OWB1:
                return composite(
                    workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-web"),
                    workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-web-openwebbeans"),
                    mavenBundle("org.apache.openwebbeans", "openwebbeans-web").versionAsInProject(),
                    mavenBundle("org.apache.openwebbeans", "openwebbeans-el22").versionAsInProject(),
                    // needed by pax-cdi-web-openwebbeans
                    mavenBundle("org.apache.geronimo.specs", "geronimo-jsp_2.2_spec", "1.2")
                    );

            case WELD2:
                return composite(
                    workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-web"),
                    workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-web-weld"),
                    mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec", "1.0"),
                    // needed by pax-cdi-web-weld
                    mavenBundle("org.apache.geronimo.specs", "geronimo-jsp_2.2_spec", "1.2")
                    );

            default:
                throw new IllegalArgumentException("pax.cdi.provider unknown or null");
        }
    }

    public static Option paxCdiProviderUndertowAdapter() {
        switch (getCdiProvider()) {

            case OWB1:
                return composite(
                    workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-servlet"),
                    workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-undertow-openwebbeans"),
                    mavenBundle("org.apache.openwebbeans", "openwebbeans-web").versionAsInProject(),
                    mavenBundle("org.apache.openwebbeans", "openwebbeans-el22").versionAsInProject(),
                    // needed by pax-cdi-web-openwebbeans
                    mavenBundle("org.apache.geronimo.specs", "geronimo-jsp_2.2_spec", "1.2")
                    );

            case WELD2:
                return composite(
                    workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-servlet"),
                    workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-undertow-weld"),
                    mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec", "1.0"),
                    // needed by pax-cdi-web-weld
                    mavenBundle("org.apache.geronimo.specs", "geronimo-jsp_2.2_spec", "1.2")
                    );

            default:
                throw new IllegalArgumentException("pax.cdi.provider unknown or null");
        }
    }

    public static Option paxCdiProviderJettyAdapter() {
        switch (getCdiProvider()) {

            case OWB1:
                return composite(
                    workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-servlet"),
                    workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-jetty-openwebbeans"),
                    mavenBundle("org.apache.openwebbeans", "openwebbeans-web").versionAsInProject(),
                    mavenBundle("org.apache.openwebbeans", "openwebbeans-el22").versionAsInProject()
                    );

            case WELD2:
                return composite(
                    workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-servlet"),
                    workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-jetty-weld")
                    );

            default:
                throw new IllegalArgumentException("pax.cdi.provider unknown or null");
        }
    }

    public static Option paxCdiJsfAdapter() {
        switch (getCdiProvider()) {

            case OWB1:
                return composite(
                    mavenBundle("org.apache.openwebbeans", "openwebbeans-jsf").versionAsInProject()
                    );

            case WELD2:
                return composite(
                    );

            default:
                throw new IllegalArgumentException("pax.cdi.provider unknown or null");
        }
    }

    public static CdiProvider getCdiProvider() {
        String provider = System.getProperty("pax.cdi.provider", "owb1");
        if (provider == null) {
            throw new IllegalArgumentException("system property pax.cdi.provider must not be null");
        }
        return CdiProvider.valueOf(provider.toUpperCase());
    }

    public static Option paxCdiCoreBundles() {
        return composite(
                mavenBundle("javax.interceptor", "javax.interceptor-api", "1.2"),
                mavenBundle("javax.annotation", "javax.annotation-api", "1.2"),
                mavenBundle("javax.enterprise", "cdi-api").versionAsInProject(),
//                mavenBundle("org.apache.geronimo.specs", "geronimo-atinject_1.0_spec", "1.0"),
                mavenBundle("javax.el", "javax.el-api", "3.0.0"),
                mavenBundle("org.apache.xbean", "xbean-bundleutils", "4.4"),
                mavenBundle("org.apache.xbean", "xbean-asm5-shaded", "4.4"),
                mavenBundle("org.apache.xbean", "xbean-finder-shaded", "4.4")
        );
    }

    public static Option openWebBeans16Bundles() {
        return composite(
            paxCdiCoreBundles(),
            mavenBundle("javax.validation", "validation-api", "1.1.0.Final"),
            mavenBundle("javax.transaction", "javax.transaction-api", "1.2"),
            mavenBundle("javax.servlet", "javax.servlet-api", "3.1.0"),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-impl").versionAsInProject(),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-spi").versionAsInProject(),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-el22").versionAsInProject(),
            mavenBundle("org.slf4j", "jul-to-slf4j").versionAsInProject());
    }

    public static Option weld2Bundles() {
        return composite(
            paxCdiCoreBundles(),
            mavenBundle("org.jboss.logging", "jboss-logging", "3.3.0.Final"),
            mavenBundle("org.jboss.classfilewriter", "jboss-classfilewriter", "1.1.2.Final"),
            mavenBundle("com.google.guava", "guava", "18.0"),
            mavenBundle("org.jboss.weld", "weld-osgi-bundle").versionAsInProject());
    }

    public static Option paxWebBundles() {
        return composite(
            mavenBundle("org.apache.xbean", "xbean-asm5-shaded", "4.4"), //
            mavenBundle("org.apache.xbean", "xbean-finder-shaded", "4.4"), //
            mavenBundle("org.apache.xbean", "xbean-bundleutils", "4.4"),
            mavenBundle("javax.servlet", "javax.servlet-api", "3.1.0"),
            mavenBundle("org.ops4j.pax.web", "pax-web-spi").version(Info.getPaxWebVersion()),
            mavenBundle("org.ops4j.pax.web", "pax-web-api").version(Info.getPaxWebVersion()),
            mavenBundle("org.ops4j.pax.web", "pax-web-extender-war").version(Info.getPaxWebVersion()).startLevel(10),
            mavenBundle("org.ops4j.pax.web", "pax-web-extender-whiteboard").version(Info.getPaxWebVersion()),
            mavenBundle("org.ops4j.pax.web", "pax-web-jetty").version(Info.getPaxWebVersion()),
            mavenBundle("org.ops4j.pax.web", "pax-web-descriptor").version(Info.getPaxWebVersion()),
            //mavenBundle("org.ops4j.pax.web", "pax-web-jsp").version(Info.getPaxWebVersion()),
            mavenBundle("org.ops4j.pax.web", "pax-web-runtime").version(Info.getPaxWebVersion()),
            mavenBundle("org.eclipse.jdt.core.compiler", "ecj").version("3.5.1"),
            mavenBundle("org.eclipse.jetty", "jetty-util").version(JETTY_VERSION),
            mavenBundle("org.eclipse.jetty", "jetty-io").version(JETTY_VERSION),
            mavenBundle("org.eclipse.jetty", "jetty-http").version(JETTY_VERSION),
            mavenBundle("org.eclipse.jetty", "jetty-continuation").version(JETTY_VERSION),
            mavenBundle("org.eclipse.jetty", "jetty-server").version(JETTY_VERSION),
            mavenBundle("org.eclipse.jetty", "jetty-security").version(JETTY_VERSION),
            mavenBundle("org.eclipse.jetty", "jetty-xml").version(JETTY_VERSION),
            mavenBundle("org.eclipse.jetty", "jetty-servlet").version(JETTY_VERSION));
    }

    public static Option paxWebUndertowBundles() {
        return composite(
            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.undertow.servlet", "1.0.15.1"),
            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.undertow.core", "1.0.15.1"),
            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.xnio.api", "3.2.2.2"),
            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.xnio.nio", "3.2.2.2"),
            mavenBundle("org.jboss.logging", "jboss-logging", "3.3.0.Final"),
            mavenBundle("javax.annotation", "javax.annotation-api", "1.2"),
            mavenBundle("javax.servlet", "javax.servlet-api", "3.1.0"),
            mavenBundle("org.apache.xbean", "xbean-bundleutils", "4.4"),
            mavenBundle("org.apache.xbean", "xbean-finder", "4.4"),
            mavenBundle("org.ow2.asm", "asm-all", "5.0.2"),
            mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.8.0"),
            mavenBundle("org.apache.felix", "org.apache.felix.eventadmin", "1.3.2"),

            mavenBundle("org.ops4j.pax.web", "pax-web-extender").version(Info.getPaxWebVersion()),
            mavenBundle("org.ops4j.pax.web", "pax-web-spi").version(Info.getPaxWebVersion()),
            mavenBundle("org.ops4j.pax.web", "pax-web-descriptor").version(Info.getPaxWebVersion()),
            mavenBundle("org.ops4j.pax.web", "pax-web-jaas").version(Info.getPaxWebVersion()),
            mavenBundle("org.ops4j.pax.web", "pax-web-undertow").version(Info.getPaxWebVersion()));
    }

    public static ProvisionOption<?> workspaceBundle(String groupId, String artifactId) {
        String samples = groupId.endsWith(".samples") ? "pax-cdi-samples/" : "";
        String fileName = String.format("%s/../../../../%s%s/target/classes",
            PathUtils.getBaseDir(), samples, artifactId);

        if (new File(fileName).exists()) {
            String url = "reference:file:" + fileName;
            return bundle(url);
        }
        else {
            return mavenBundle(groupId, artifactId, Info.getPaxCdiVersion());
        }
    }

    public static Option workspaceFragment(String groupId, String artifactId) {
        String samples = groupId.endsWith(".samples") ? "pax-cdi-samples/" : "";
        String fileName = String.format("%s/../../../../%s%s/target/classes",
            PathUtils.getBaseDir(), samples, artifactId);

        if (new File(fileName).exists()) {
            String url = "reference:file:" + fileName;
            return bundle(url).noStart();
        }
        else {
            return mavenBundle(groupId, artifactId, Info.getPaxCdiVersion()).noStart();
        }
    }

    public static String getPaxCdiRoot() {
        if (paxCdiRoot == null) {
            paxCdiRoot = System.getProperty("pax.cdi.root");
            if (paxCdiRoot == null) {
                Properties props = new Properties();
                try {
                    props.load(TestConfiguration.class
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

    public static boolean isWeld() {
        switch (getCdiProvider()) {
            case WELD2:
                return true;
            default:
                return false;
        }
    }

    public static boolean isEquinox() {
        FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
        return factory.getClass().getSimpleName().contains("Equinox");
    }

    public static boolean isFelix() {
        FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
        return factory.getClass().getCanonicalName().contains("felix");
    }
}
