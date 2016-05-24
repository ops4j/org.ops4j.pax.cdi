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
import javax.enterprise.inject.Any;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.ops4j.pax.cdi.api2.Component;
import org.ops4j.pax.cdi.api2.Service;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;

public class MandatoryLazyReferenceTest extends AbstractTest {

    @Test
    @Ignore
    public void test() throws Exception {
        createCdi(Hello.class);

        Assert.assertEquals(0, Hello.created.get());
        Assert.assertEquals(0, Hello.destroyed.get());

        ServiceRegistration<MyService> registration = register(MyService.class, () -> "Hello world !!");

        Assert.assertEquals(0, Hello.created.get());
        Assert.assertEquals(0, Hello.destroyed.get());
        Assert.assertNull(Hello.instance.get());

        // TODO: how to get the bean correctly ?
        Hello hello = welds.iterator().next().select(Hello.class, AnyLiteral.INSTANCE).get();

        Assert.assertEquals(1, Hello.created.get());
        Assert.assertEquals(0, Hello.destroyed.get());
        Assert.assertNotNull(Hello.instance.get());

        registration.unregister();

        Assert.assertEquals(1, Hello.created.get());
        Assert.assertEquals(1, Hello.destroyed.get());
        Assert.assertNull(Hello.instance.get());
    }

    static class AnyLiteral extends AnnotationLiteral<Any> implements Any {

        public static AnyLiteral INSTANCE = new AnyLiteral();

    }

    public interface MyService {

        String hello();

    }

    @Component
    public static class Hello {

        static final AtomicInteger created = new AtomicInteger();
        static final AtomicInteger destroyed = new AtomicInteger();
        static final AtomicReference<Hello> instance = new AtomicReference<>();

        @Inject @Service MyService service;

        @PostConstruct
        public void init() {
            created.incrementAndGet();
            instance.compareAndSet(null, this);
            System.err.println("Creating Hello instance");
        }

        @PreDestroy
        public void destroy() {
            destroyed.incrementAndGet();
            instance.compareAndSet(this, null);
            System.err.println("Destroying Hello instance");
        }

        public String sayHelloWorld() {
            return service.hello();
        }
    }

}
