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

import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;

import org.ops4j.pax.cdi.api.Info;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.UrlProvisionOption;
import org.ops4j.pax.exam.util.PathUtils;

/**
 * Reusable composite options for Pax CDI integration tests with Pax Exam.
 *  
 * @author Harald Wellmann
 */
public class TestConfiguration {
    
    public static final String WELD1 = "weld1";
    public static final String OWB1 = "owb1";
    
    
    private static final String JETTY_VERSION = "8.1.9.v20130131";

    private TestConfiguration() {
    }

    public static Option regressionDefaults() {
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

            mavenBundle("org.apache.felix", "org.apache.felix.scr", "1.6.2"),

            // Set logback configuration via system property.
            // This way, both the driver and the container use the same configuration
            systemProperty("logback.configurationFile").value(
                "file:" + PathUtils.getBaseDir() + "/src/test/resources/logback.xml"),
            
            systemProperty("osgi.console").value("6666"),             
            systemProperty("eclipse.consoleLog").value("true"),
            
            systemProperty("osgi.java.profile").value("J2SE-1.5.profile"),
            systemPackages("javax.activation", 
            	"javax.annotation.processing",
            	"javax.lang.model",
            	"javax.lang.model.element",
            	"javax.lang.model.type",
            	"javax.lang.model.util",
            	"javax.tools",
            	"javax.xml.bind",
            	"javax.xml.bind.annotation",
            	"javax.xml.stream"
            	),
            
            systemTimeout(2000000),
            junitBundles());
    }

    public static Option cdiProviderBundles() {
        switch (getCdiProvider()) {

            case OWB1:
                return openWebBeansBundles();
            
            case WELD1:    
                return weldBundles();
        }
        throw new IllegalArgumentException("pax.cdi.provider unknown or null");
    }

    public static Option paxCdiProviderAdapter() {
        switch (getCdiProvider()) {

            case OWB1:
                return workspaceBundle("pax-cdi-openwebbeans");
            
            case WELD1:    
                return workspaceBundle("pax-cdi-weld");
        }
        throw new IllegalArgumentException("pax.cdi.provider unknown or null");
    }

    public static Option paxCdiProviderWebAdapter() {
        switch (getCdiProvider()) {

            case OWB1:
                return composite(
                    workspaceBundle("pax-cdi-web-openwebbeans"),
                    mavenBundle("org.apache.openwebbeans", "openwebbeans-web").versionAsInProject(),
                    mavenBundle("org.apache.openwebbeans", "openwebbeans-el22").versionAsInProject()
                    );
            
            case WELD1:    
                return composite(
                    workspaceBundle("pax-cdi-web-weld"),
                    mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec").versionAsInProject()
                    );
        }
        throw new IllegalArgumentException("pax.cdi.provider unknown or null");
    }

    public static CdiProvider getCdiProvider() {
        String provider = System.getProperty("pax.cdi.provider");
        if (provider == null) {
            throw new IllegalArgumentException("system property pax.cdi.provider must not be null");
        }
        return CdiProvider.valueOf(provider.toUpperCase());
    }

    public static Option openWebBeansBundles() {
        return composite(
            mavenBundle("org.apache.openwebbeans", "openwebbeans-impl").versionAsInProject(),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-spi").versionAsInProject(),
            mavenBundle("org.apache.xbean", "xbean-bundleutils").versionAsInProject(),
            mavenBundle("org.apache.xbean", "xbean-asm-shaded").versionAsInProject(), //
            mavenBundle("org.apache.xbean", "xbean-finder-shaded").versionAsInProject(), //
            mavenBundle("org.slf4j", "jul-to-slf4j").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-annotation_1.1_spec", "1.0.1"),
            mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec")
            .versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-validation_1.0_spec")
                .versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jcdi_1.0_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-interceptor_1.1_spec")
                .versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-el_2.2_spec").versionAsInProject());
    }

    public static Option weldBundles() {
        return composite(
            workspaceBundle("pax-cdi-weld"),

            mavenBundle("ch.qos.cal10n", "cal10n-api", "0.7.4"),
            mavenBundle("org.apache.xbean", "xbean-bundleutils").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-annotation_1.1_spec", "1.0.1"),
            mavenBundle("org.apache.geronimo.specs", "geronimo-interceptor_1.1_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-el_2.2_spec").versionAsInProject(),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.asm").versionAsInProject(), //
            mavenBundle("org.jboss.weld", "weld-osgi-bundle").versionAsInProject().startLevel(3));
    }

    public static Option paxWebBundles() {
        return composite(
            mavenBundle("org.apache.xbean", "xbean-asm-shaded").versionAsInProject(), //
            mavenBundle("org.apache.xbean", "xbean-finder-shaded").versionAsInProject(), //
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
            mavenBundle("org.eclipse.jetty", "jetty-util").version(JETTY_VERSION),
            mavenBundle("org.eclipse.jetty", "jetty-io").version(JETTY_VERSION),
            mavenBundle("org.eclipse.jetty", "jetty-http").version(JETTY_VERSION),
            mavenBundle("org.eclipse.jetty", "jetty-continuation").version(JETTY_VERSION),
            mavenBundle("org.eclipse.jetty", "jetty-server").version(JETTY_VERSION),
            mavenBundle("org.eclipse.jetty", "jetty-security").version(JETTY_VERSION),
            mavenBundle("org.eclipse.jetty", "jetty-xml").version(JETTY_VERSION),
            mavenBundle("org.eclipse.jetty", "jetty-servlet").version(JETTY_VERSION));
    }

    public static UrlProvisionOption workspaceBundle(String pathFromRoot) {
        String url = String.format("reference:file:%s/../%s/target/classes",
            PathUtils.getBaseDir(), pathFromRoot);
        return bundle(url);
    }
}
