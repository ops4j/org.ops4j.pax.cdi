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

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.spi.scan.BeanDescriptorParser;
import org.ops4j.pax.cdi.spi.scan.BeanDiscoveryMode;
import org.ops4j.pax.cdi.spi.scan.BeanScanner;
import org.ops4j.pax.cdi.spi.scan.DefaultBeanDescriptor;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.osgi.framework.Bundle;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BeanScannerTest extends AbstractControlledTestBase {

    @Configuration
    public Option[] config() {
        return combine(
                baseConfigure(),
//                regressionDefaults(),

                // This is a bundle with embedded JARs on the bundle classpath
                mavenBundle("org.ops4j.pax.tinybundles", "tinybundles", "2.1.1"),
                mavenBundle("biz.aQute.bnd", "bndlib", "2.4.0")
        );
    }

    @Test
    public void archiveWithoutExtenderShouldBeEmpty() {
        Bundle bundle = BundleUtils.getBundle(bc, "org.ops4j.pax.tinybundles");
        assertThat(bundle, is(notNullValue()));

        BeanDescriptorParser parser = beansXml -> new DefaultBeanDescriptor(null, BeanDiscoveryMode.ALL, "1.0");

        BeanScanner scanner = new BeanScanner(bundle, parser);
        scanner.scan();
        Set<String> beanClasses = scanner.getBeanClasses();
        assertThat(beanClasses.size(), is(0));
    }
}
