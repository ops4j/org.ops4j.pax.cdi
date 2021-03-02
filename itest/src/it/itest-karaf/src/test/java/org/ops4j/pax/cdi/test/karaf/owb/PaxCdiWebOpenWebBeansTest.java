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
package org.ops4j.pax.cdi.test.karaf.owb;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.karaf.RegressionConfiguration.PAX_CDI_FEATURES;
import static org.ops4j.pax.cdi.test.karaf.RegressionConfiguration.PAX_WEB_FEATURES;
import static org.ops4j.pax.cdi.test.karaf.RegressionConfiguration.SAMPLE1;
import static org.ops4j.pax.cdi.test.karaf.RegressionConfiguration.SAMPLE1_WEB;
import static org.ops4j.pax.cdi.test.karaf.RegressionConfiguration.regressionDefaults;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

@RunWith(PaxExam.class)
public class PaxCdiWebOpenWebBeansTest {

    @Inject
    private CdiContainerFactory factory;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),
            features(PAX_WEB_FEATURES, "pax-http"),
            features(PAX_WEB_FEATURES, "pax-http-whiteboard"),
            features(PAX_CDI_FEATURES, "pax-cdi-web-openwebbeans"),
            SAMPLE1,
            SAMPLE1_WEB);
    }

    @Test
    public void test() throws Exception {
        assertThat(factory, is(notNullValue()));
    }
}
