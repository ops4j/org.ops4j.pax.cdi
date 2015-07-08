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

import static org.ops4j.pax.cdi.test.support.TestConfiguration.cdiProviderBundles;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import javax.inject.Inject;

import org.apache.deltaspike.security.api.authorization.AccessDeniedException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.sample8.service.SecuredClient;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SecurityTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Inject
    private SecuredClient securedService;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),
            paxCdiProviderAdapter(),
            cdiProviderBundles(),

            mavenBundle("org.osgi", "org.osgi.enterprise").versionAsInProject(),

            // DeltaSpike bundles
            mavenBundle("org.apache.deltaspike.core", "deltaspike-core-api").versionAsInProject(),
            mavenBundle("org.apache.deltaspike.core", "deltaspike-core-impl").versionAsInProject(),

            mavenBundle("org.apache.deltaspike.modules", "deltaspike-security-module-api").versionAsInProject(),
            mavenBundle("org.apache.deltaspike.modules", "deltaspike-security-module-impl").versionAsInProject(),

//            wrappedBundle(mavenBundle("org.apache.deltaspike.modules", "deltaspike-security-module-api")
//                .versionAsInProject())
//                .instructions(
//                    "overwrite=merge",
//                    "Bundle-SymbolicName=org.apache.deltaspike.modules.deltaspike-security-module-api"),
//            wrappedBundle(mavenBundle("org.apache.deltaspike.modules", "deltaspike-security-module-impl").versionAsInProject())
//                .instructions(
//                    "overwrite=merge",
//                    "Bundle-SymbolicName=org.apache.deltaspike.modules.deltaspike-security-module-impl",
//                    "Provide-Capability=org.ops4j.pax.cdi.extension;extension=\"deltaspike-security-module-impl\"",
//                    "Require-Capability=org.ops4j.pax.cdi.extension; filter:=\"(extension=pax-cdi-extension)\", "
//                    + "osgi.extender; filter:=\"(osgi.extender=pax.cdi)\""),

            // Sample bundles
            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample8"));
    }

    @Test
    public void shouldNotInvokeBlockedService() {
        thrown.expect(AccessDeniedException.class);
        securedService.getBlockedResult();
    }
}
