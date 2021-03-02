/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ops4j.pax.cdi.test;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.ops4j.pax.cdi.test.support.TestConfiguration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.cdi.test.support.TestConfiguration.PAX_CDI_VERSION;
import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.url;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;

/**
 * <p>Test base to be used with <code>pax.exam.system=default</code>, where no implicit bundles
 * are installed by pax-exam itself - all bundles have to be installed explicitly</p>
 */
public class AbstractControlledTestBase {

    protected static final Logger LOG = LoggerFactory.getLogger("org.ops4j.pax.cdi.itest");

    @Rule
    public TestName testName = new TestName();

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    protected BundleContext bc;

    @Before
    public void beforeEach() {
        LOG.info("========== Running {}.{}() ==========", getClass().getName(), testName.getMethodName());
    }

    @After
    public void afterEach() {
        LOG.info("========== Finished {}.{}() ==========", getClass().getName(), testName.getMethodName());
    }

    protected static Option[] baseConfigure() {
        return options(
                // basic options
                bootDelegationPackage("sun.*"),
                bootDelegationPackage("com.sun.*"),
                frameworkStartLevel(20),
                workingDirectory("target/paxexam"),
                cleanCaches(true),
                systemTimeout(60 * 60 * 1000),

                // path relative to pax-web-itest-container-<containerName>
                systemProperty("org.ops4j.pax.logging.property.file").value("src/test/resources/pax-logging.properties"),
                frameworkProperty("felix.bootdelegation.implicit").value("false"),
                // set to "4" to see Felix wiring information
                frameworkProperty("felix.log.level").value("1"),

                // added implicitly by pax-exam, if pax.exam.system=test
                // these resources are provided inside org.ops4j.pax.exam:pax-exam-link-mvn jar
                url("link:classpath:META-INF/links/org.ops4j.base.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.core.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.extender.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.framework.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.lifecycle.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.tracker.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.exam.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.exam.inject.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.extender.service.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),

                // configadmin should start before org.ops4j.pax.logging.pax-logging-log4j2
                linkBundle("org.apache.felix.configadmin").startLevel(START_LEVEL_SYSTEM_BUNDLES),

                // added implicitly by pax-exam, if pax.exam.system=test
                //  - url("link:classpath:META-INF/links/org.ops4j.pax.logging.api.link").startLevel( START_LEVEL_SYSTEM_BUNDLES),
                //  - url("link:classpath:META-INF/links/org.apache.geronimo.specs.atinject.link") .startLevel(START_LEVEL_SYSTEM_BUNDLES),
                //  - url("link:classpath:META-INF/links/org.osgi.compendium.link").startLevel( START_LEVEL_SYSTEM_BUNDLES),
                // but we will use versions aligned to pax-web:
                linkBundle("org.ops4j.pax.logging.pax-logging-api").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                linkBundle("org.ops4j.pax.logging.pax-logging-log4j2").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                linkBundle("org.apache.servicemix.bundles.javax-inject").startLevel(START_LEVEL_SYSTEM_BUNDLES),

                // org.ops4j.pax.exam.nat.internal.NativeTestContainer.start() adds this explicitly
                systemProperty("java.protocol.handler.pkgs").value("org.ops4j.pax.url"),

                // last Option[] that's required to run simplest @Test
                junitBundles(),

                systemProperty("ProjectVersion").value(PAX_CDI_VERSION),

                TestConfiguration.cdiProviderBundles()

//                mavenBundle().groupId("javax.servlet").artifactId("javax.servlet-api").versionAsInProject(),
//                mavenBundle().groupId("javax.websocket").artifactId("javax.websocket-api").version(asInProject()),
//                paxWebBundles(),
        );
    }

}
