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
package org.ops4j.pax.cdi.extension2;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.ops4j.pax.cdi.api2.Component;
import org.ops4j.pax.cdi.api2.Greedy;
import org.ops4j.pax.cdi.api2.Immediate;
import org.ops4j.pax.cdi.api2.Service;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;

public class InstanceStaticGreedyReferenceTest extends AbstractTest {

    @Test
    public void test() throws Exception {
        createCdi(Hello.class);

        Assert.assertEquals(0, Hello.created.get());
        Assert.assertEquals(0, Hello.destroyed.get());
        Assert.assertNull(Hello.instance.get());

        ServiceRegistration<MyService> registration1 = register(MyService.class, () -> "Hello 1 !!");

        Assert.assertEquals(1, Hello.created.get());
        Assert.assertEquals(0, Hello.destroyed.get());
        Assert.assertEquals("Hello 1 !!", Hello.instance.get().sayHelloWorld());

        ServiceRegistration<MyService> registration2 = register(MyService.class, () -> "Hello 2 !!");

        Assert.assertEquals(2, Hello.created.get());
        Assert.assertEquals(1, Hello.destroyed.get());
        Assert.assertEquals("Hello 1 !!\nHello 2 !!", Hello.instance.get().sayHelloWorld());

        registration1.unregister();

        Assert.assertEquals(3, Hello.created.get());
        Assert.assertEquals(2, Hello.destroyed.get());
        Assert.assertEquals("Hello 2 !!", Hello.instance.get().sayHelloWorld());

        registration2.unregister();

        Assert.assertEquals(3, Hello.created.get());
        Assert.assertEquals(3, Hello.destroyed.get());
        Assert.assertNull(Hello.instance.get());
    }

    public interface MyService {

        String hello();

    }

    @Immediate @Component
    public static class Hello {

        static final AtomicInteger created = new AtomicInteger();
        static final AtomicInteger destroyed = new AtomicInteger();
        static final AtomicReference<Hello> instance = new AtomicReference<>();

        @Inject @Greedy @Service
        Instance<MyService> service;

        @PostConstruct
        public void init() {
            created.incrementAndGet();
            instance.set(this);
            System.err.println("Creating Hello instance");
        }

        @PreDestroy
        public void destroy() {
            System.err.println("Destroying Hello instance");
            destroyed.incrementAndGet();
            instance.set(null);
        }

        public String sayHelloWorld() {
            List<String> strings = new ArrayList<>();
            service.forEach(s -> strings.add(s.hello()));
            Collections.sort(strings); // TODO: ordering ?
            return String.join("\n", strings);
        }
    }

}
