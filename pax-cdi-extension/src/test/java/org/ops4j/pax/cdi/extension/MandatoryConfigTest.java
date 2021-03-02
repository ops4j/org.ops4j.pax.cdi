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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.Config;
import org.ops4j.pax.cdi.api.Immediate;

public class MandatoryConfigTest extends AbstractTest {

    @Test(timeout = 10000)
    public void test() throws Exception {
        startConfigAdmin();

        synchronized (Hello.INSTANCE) {
            createCdi(Hello.class);

            Assert.assertEquals(0, Hello.CREATED.get());
            Assert.assertEquals(0, Hello.DESTROYED.get());

            // create configuration
            getConfiguration(MyConfig.class).update(dictionary("host", "localhost"));

            Hello.INSTANCE.wait();

            Assert.assertEquals("Hello world at localhost:8234", Hello.INSTANCE.get().sayHelloWorld());

            Assert.assertEquals(1, Hello.CREATED.get());
            Assert.assertEquals(0, Hello.DESTROYED.get());

            // delete configuration
            getConfiguration(MyConfig.class).delete();

            Hello.INSTANCE.wait();

            Assert.assertEquals(1, Hello.CREATED.get());
            Assert.assertEquals(1, Hello.DESTROYED.get());
        }
    }

    @interface MyConfig {

        String host() default "0.0.0.0";
        int port() default 8234;

    }

    @Immediate @Component
    public static class Hello {

        static final AtomicInteger CREATED = new AtomicInteger();
        static final AtomicInteger DESTROYED = new AtomicInteger();
        static final AtomicReference<Hello> INSTANCE = new AtomicReference<>();

        @Inject @Config
        MyConfig config;

        @PostConstruct
        public void init() {
            CREATED.incrementAndGet();
            INSTANCE.set(this);
            System.err.println("Creating Hello instance");
            synchronized (INSTANCE) {
                INSTANCE.notifyAll();
            }
        }

        @PreDestroy
        public void destroy() {
            DESTROYED.incrementAndGet();
            INSTANCE.set(null);
            System.err.println("Destroying Hello instance");
            synchronized (INSTANCE) {
                INSTANCE.notifyAll();
            }
        }

        public String sayHelloWorld() {
            return "Hello world at " + config.host() + ":" + config.port();
        }
    }

}
