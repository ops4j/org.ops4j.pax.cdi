/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ops4j.pax.cdi.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.cdiProviderBundles;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.paxCdiProviderAdapter;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.cdi.test.support.TestConfiguration.workspaceBundle;
import org.ops4j.pax.exam.Configuration;
import static org.ops4j.pax.exam.CoreOptions.options;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class InterBundleProducesTest {

//    @Inject
//    @OsgiService
//    private Logger logger;
    @Configuration
    public Option[] config() {
        return options(
                regressionDefaults(),
                workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample10"),
                workspaceBundle("org.ops4j.pax.cdi.samples", "pax-cdi-sample10-client"),
                paxCdiProviderAdapter(),
                cdiProviderBundles());
    }

    @Test
    public void testParameterizedTypeSupported() {
//        assertNotNull(logger);
//        assertEquals(InterBundleProducesTest.class.getName(), logger.getName());
    }

}
