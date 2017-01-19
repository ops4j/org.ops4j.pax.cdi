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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.Archive.Entry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.api.Service;
import org.ops4j.pax.cdi.spi.scan.BundleArchive;
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
public class BundleArchiveTest {

    @Inject
    private BundleContext bc;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),

            // This is a bundle with embedded JARs on the bundle classpath
            mavenBundle("org.ops4j.pax.tinybundles", "tinybundles", "1.0.0"),

            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-api"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-spi"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-extender"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-extension"),
            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample6"),
            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample1"),
            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample1-client"),

            mavenBundle("org.apache.xbean", "xbean-bundleutils").versionAsInProject(),
            mavenBundle("org.apache.xbean", "xbean-finder-shaded").versionAsInProject(),
            mavenBundle("org.apache.xbean", "xbean-asm5-shaded").versionAsInProject(),
            mavenBundle("javax.annotation", "javax.annotation-api", "1.2"),
            mavenBundle("javax.interceptor", "javax.interceptor-api", "1.2"),
            mavenBundle("javax.el", "javax.el-api", "3.0.0"),
            mavenBundle("javax.enterprise", "cdi-api").versionAsInProject());
    }

    @Test
    public void shouldFindClassFromEmbeddedJar() throws ClassNotFoundException, IOException {
        Bundle bundle = BundleUtils.getBundle(bc, "org.ops4j.pax.tinybundles");
        assertThat(bundle, is(notNullValue()));

        BundleArchive archive = new BundleArchive(bundle);
        Iterator<Entry> it = archive.iterator();
        while (it.hasNext()) {
            String className = it.next().getName();
            System.out.println(className);
            assertThat(archive.getBytecode(className), is(notNullValue()));
        }
    }

    @Test
    public void shouldFindClassFromImportedPackage() throws ClassNotFoundException, IOException {
        Bundle bundle = BundleUtils.getBundle(bc, "org.ops4j.pax.cdi.spi");
        assertThat(bundle, is(notNullValue()));

        BundleArchive archive = new BundleArchive(bundle);
        Iterator<Entry> it = archive.iterator();
        while (it.hasNext()) {
            System.out.println(it.next().getName());
        }
    }

    @Test
    public void shouldFindAnnotations() {
        Bundle bundle = BundleUtils.getBundle(bc, "org.ops4j.pax.cdi.sample1.client");
        assertThat(bundle, is(notNullValue()));

        BundleArchive archive = new BundleArchive(bundle);
        AnnotationFinder finder = new AnnotationFinder(archive);
        List<Class<?>> classes = finder.findAnnotatedClasses(Service.class);
        System.out.println(classes);
        assertThat(classes.size(), is(2));
    }
}
