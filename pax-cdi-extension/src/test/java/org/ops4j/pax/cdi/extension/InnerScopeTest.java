/*
 * Copyright 2021 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.ops4j.pax.cdi.api.PrototypeScoped;
import org.ops4j.pax.cdi.api.Service;

public class InnerScopeTest extends AbstractTest {

    @Test
    public void test() throws Exception {
        createCdi(Hello.class, Component1.class, Component2.class);

        Assert.assertEquals(0, Hello.CREATED.get());
        Assert.assertEquals(0, Hello.DESTROYED.get());

        Component1 component1 = getService(Component1.class);
        Component2 component2 = getService(Component2.class);

        Assert.assertNotNull(component1);
        Assert.assertNotNull(component2);
        Assert.assertNotNull(component1.service);
        Assert.assertNotNull(component2.service);
        Assert.assertNotSame(component1.service, component2.service);
    }

    public interface MyService {

    }

    @Service @Component
    public static class Component1 {

        @Inject @Component
        Hello service;

    }

    @Service @Component
    public static class Component2 {

        @Inject @Component
        Hello service;

    }

    @Component @PrototypeScoped
    public static class Hello implements MyService {

        static final AtomicInteger CREATED = new AtomicInteger();
        static final AtomicInteger DESTROYED = new AtomicInteger();
        static final AtomicReference<Hello> INSTANCE = new AtomicReference<>();

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
            return "Hello world !!";
        }
    }

}
