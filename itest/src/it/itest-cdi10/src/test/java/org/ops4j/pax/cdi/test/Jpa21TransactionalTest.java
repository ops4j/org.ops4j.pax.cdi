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
package org.ops4j.pax.cdi.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.cdiProviderBundles;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.workspaceBundle;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.sample2.service.LibraryServiceClient;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.UrlProvisionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.jpa.sample1.model.Author;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class Jpa21TransactionalTest {

    private static final String HIBERNATE_VERSION = "4.3.5.Final";

    @Inject
    private LibraryServiceClient libraryService;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),
            paxCdiProviderAdapter(),
            cdiProviderBundles(),

            // Hibernate and dependencies
            systemProperty("org.jboss.logging.provider").value("slf4j"),
            systemPackages("javax.xml.stream; version=\"1.0.0\"",
                "javax.xml.stream.events; version=\"1.0.0\"",
                "javax.xml.stream.util; version=\"1.0.0\""),
            mavenBundle("org.hibernate.javax.persistence", "hibernate-jpa-2.1-api", "1.0.0.Final"),
            mavenBundle("javax.validation", "validation-api", "1.1.0.Final"),

            mavenBundle("org.hibernate.common", "hibernate-commons-annotations", "4.0.4.Final"),
            mavenBundle("org.hibernate", "hibernate-core", HIBERNATE_VERSION),
            mavenBundle("org.hibernate", "hibernate-osgi", HIBERNATE_VERSION),
            mavenBundle("org.hibernate", "hibernate-entitymanager", HIBERNATE_VERSION),

            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.antlr",
                "2.7.7_5"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.dom4j",
                "1.6.1_5"), //
            mavenBundle("org.javassist", "javassist", "3.18.1-GA"),
            mavenBundle("com.fasterxml", "classmate", "0.5.4"),            
            mavenBundle("org.jboss", "jandex", "1.2.0.Final"),            
            mavenBundle("org.jboss.logging", "jboss-logging", "3.1.0.GA"),

            // Pax JPA, Pax JDBC and Derby driver
            mavenBundle("org.ops4j.pax.jpa", "pax-jpa").versionAsInProject().startLevel(2),
            mavenBundle("org.ops4j.pax.jdbc", "pax-jdbc").versionAsInProject(),
            mavenBundle("org.apache.derby", "derby").versionAsInProject(),
            mavenBundle("org.osgi", "org.osgi.enterprise").versionAsInProject(),


            // DeltaSpike
            mavenBundle("org.apache.deltaspike.core", "deltaspike-core-api").versionAsInProject(),
            mavenBundle("org.apache.deltaspike.core", "deltaspike-core-impl").versionAsInProject(),
            mavenBundle("org.apache.deltaspike.modules", "deltaspike-jpa-module-api").versionAsInProject(),
            mavenBundle("org.apache.deltaspike.modules", "deltaspike-partial-bean-module-api").versionAsInProject(),
            mavenBundle("org.apache.deltaspike.modules", "deltaspike-data-module-api").versionAsInProject(),

//            mavenBundle("org.apache.deltaspike.modules", "deltaspike-jpa-module-impl").versionAsInProject(),
//            mavenBundle("org.apache.deltaspike.modules", "deltaspike-partial-bean-module-impl").versionAsInProject(),
//            mavenBundle("org.apache.deltaspike.modules", "deltaspike-data-module-impl").versionAsInProject(),

            // DeltaSpike bundles with missing requirements and capabilities
            wrappedDeltaSpikeBundle("deltaspike-jpa-module-impl"),
            wrappedDeltaSpikeBundle("deltaspike-data-module-impl"),
            wrappedDeltaSpikeBundle("deltaspike-partial-bean-module-impl"),

            mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec").versionAsInProject(),

            // Sample bundles
            mavenBundle("org.ops4j.pax.jpa.samples", "pax-jpa-sample1").versionAsInProject(),
            workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample2-service"));
    }

    private static UrlProvisionOption wrappedDeltaSpikeBundle(String artifactId) {
        String url = mavenBundle("org.apache.deltaspike.modules", artifactId).versionAsInProject().getURL();
        return bundle(String.format("wrap:%s,file:src/test/resources/%s.bnd", url, artifactId));
    }

    @Test
    public void createAuthorInTransaction() {
        libraryService.createAuthorViaDao("Charles", "Dickens");
        Author author = libraryService.findAuthor("Charles", "Dickens");
        assertThat(author, is(notNullValue()));
        assertThat(author.getFirstName(), is("Charles"));
        assertThat(author.getLastName(), is("Dickens"));
    }
}
