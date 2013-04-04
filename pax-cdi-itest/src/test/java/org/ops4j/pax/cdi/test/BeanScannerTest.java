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
package org.ops4j.pax.cdi.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.ops4j.pax.cdi.test.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.spi.scan.BeanScanner;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BeanScannerTest {

    @Inject
    private BundleContext bc;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),

            // This is a bundle with embedded JARs on the bundle classpath
            mavenBundle("org.ops4j.pax.tinybundles", "tinybundles", "1.0.0"),

            workspaceBundle("pax-cdi-api"), workspaceBundle("pax-cdi-spi"),

            mavenBundle("org.apache.geronimo.specs", "geronimo-el_2.2_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-interceptor_1.1_spec")
                .versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jcdi_1.0_spec").versionAsInProject());
    }

    @Test
    public void checkBeanCandidateFromEmbeddedJar() {
        Bundle bundle = BundleUtils.getBundle(bc, "org.ops4j.pax.tinybundles");
        assertThat(bundle, is(notNullValue()));

        BeanScanner scanner = new BeanScanner(bundle);
        scanner.scan();        
        Set<String> beanClasses = scanner.getBeanClasses();

        // check we can see a class from an embedded JAR
        assertThat(beanClasses, hasItem("org.ops4j.store.StoreFactory"));
    }

}
