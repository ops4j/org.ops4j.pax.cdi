package org.ops4j.pax.cdi.test.karaf;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.cdi.test.karaf.RegressionConfiguration.regressionDefaults;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.api.Info;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class PaxCdiWebWeldTest {

    @Inject
    private CdiContainerFactory factory;

    @Configuration
    public Option[] config() {
        return new Option[] { 
            regressionDefaults(),
            
            features(
                maven().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-features").type("xml")
                    .classifier("features").version(Info.getPaxCdiVersion()), "pax-cdi-web-weld")            
            };
    }

    @Test
    public void test() throws Exception {
        assertThat(factory, is(notNullValue()));
    }
}
