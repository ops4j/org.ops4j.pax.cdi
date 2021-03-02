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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.Immediate;
import org.ops4j.pax.cdi.api.Service;
import org.osgi.framework.ServiceRegistration;

public class InstanceStaticReluctantReferenceTest extends AbstractTest {

    @Test
    public void test() throws Exception {
        createCdi(Hello.class);

        Assert.assertEquals(0, Hello.CREATED.get());
        Assert.assertEquals(0, Hello.DESTROYED.get());

        ServiceRegistration<MyService> registration1 = register(MyService.class, () -> "Hello 1 !!");


        Assert.assertEquals(1, Hello.CREATED.get());
        Assert.assertEquals(0, Hello.DESTROYED.get());
        Assert.assertEquals("Hello 1 !!", Hello.INSTANCE.get().sayHelloWorld());

        ServiceRegistration<MyService> registration2 = register(MyService.class, () -> "Hello 2 !!");

        Assert.assertEquals(1, Hello.CREATED.get());
        Assert.assertEquals(0, Hello.DESTROYED.get());

        registration1.unregister();

        Assert.assertEquals(2, Hello.CREATED.get());
        Assert.assertEquals(1, Hello.DESTROYED.get());

        registration2.unregister();

        Assert.assertEquals(2, Hello.CREATED.get());
        Assert.assertEquals(2, Hello.DESTROYED.get());
    }

    public interface MyService {

        String hello();

    }

    @Immediate @Component
    public static class Hello {

        static final AtomicInteger CREATED = new AtomicInteger();
        static final AtomicInteger DESTROYED = new AtomicInteger();
        static final AtomicReference<Hello> INSTANCE = new AtomicReference<>();

        @Inject @Service
        Instance<MyService> service;

        @PostConstruct
        public void init() {
            CREATED.incrementAndGet();
            INSTANCE.set(this);
            System.err.println("Creating Hello instance");
        }

        @PreDestroy
        public void destroy() {
            System.err.println("Destroying Hello instance");
            DESTROYED.incrementAndGet();
            INSTANCE.set(null);
        }

        public String sayHelloWorld() {
            final List<String> strings = new ArrayList<>();
            service.forEach(s -> strings.add(s.hello()));
            return String.join("\n", strings);
        }
    }

}
