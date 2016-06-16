/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.extension;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.Config;
import org.ops4j.pax.cdi.api.Greedy;
import org.ops4j.pax.cdi.api.Immediate;
import org.ops4j.pax.cdi.api.Optional;
import org.junit.Assert;
import org.junit.Test;

public class OptionalGreedyConfigTest extends AbstractTest {

    @Test(timeout = 1000)
    public void test() throws Exception {
        startConfigAdmin();
        createCdi(Hello.class);

        Assert.assertEquals(1, Hello.created.get());
        Assert.assertEquals(0, Hello.destroyed.get());
        Assert.assertNotNull(Hello.instance.get());
        Assert.assertEquals("Hello world at 0.0.0.0:8234", Hello.instance.get().sayHelloWorld());

        synchronized (Hello.instance) {

            // create configuration
            getConfiguration(MyConfig.class).update(dictionary("host", "localhost"));

            Hello.instance.wait();
            Assert.assertEquals(1, Hello.destroyed.get());
            Hello.instance.wait();
            Assert.assertEquals(2, Hello.created.get());
            Assert.assertEquals("Hello world at localhost:8234", Hello.instance.get().sayHelloWorld());

            // delete configuration
            getConfiguration(MyConfig.class).delete();

            Hello.instance.wait();
            Assert.assertEquals(2, Hello.destroyed.get());
            Hello.instance.wait();
            Assert.assertEquals(3, Hello.created.get());
        }
    }

    @interface MyConfig {

        String host() default "0.0.0.0";
        int port() default 8234;

    }

    @Immediate @Component
    public static class Hello {

        static final AtomicInteger created = new AtomicInteger();
        static final AtomicInteger destroyed = new AtomicInteger();
        static final AtomicReference<Hello> instance = new AtomicReference<>();

        @Inject @Optional @Config @Greedy
        MyConfig config;

        @PostConstruct
        public void init() {
            created.incrementAndGet();
            instance.set(this);
            System.err.println("Creating Hello instance");
            synchronized (instance) {
                instance.notifyAll();
            }
        }

        @PreDestroy
        public void destroy() {
            destroyed.incrementAndGet();
            instance.set(null);
            System.err.println("Destroying Hello instance");
            synchronized (instance) {
                instance.notifyAll();
            }
        }

        public String sayHelloWorld() {
            return "Hello world at " + config.host() + ":" + config.port();
        }
    }

}
