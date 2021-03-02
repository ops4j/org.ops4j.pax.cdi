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

import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.Service;
import org.ops4j.pax.cdi.api.SingletonScoped;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

public class ScopeSingletonTest extends AbstractTest {

    @Test
    public void test() throws Exception {
        createCdi(Hello.class);

        Assert.assertEquals(0, Hello.CREATED.get());
        Assert.assertEquals(0, Hello.DESTROYED.get());

        org.osgi.framework.Bundle bundle1 = getBundleContext().installBundle("bundle1",
                TinyBundles.bundle().set( Constants.BUNDLE_SYMBOLICNAME, "bundle1" ).build());
        org.osgi.framework.Bundle bundle2 = getBundleContext().installBundle("bundle2",
                TinyBundles.bundle().set( Constants.BUNDLE_SYMBOLICNAME, "bundle2" ).build());
        bundle1.start();
        bundle2.start();

        ServiceReference<MyService> ref1 = bundle1.getBundleContext().getServiceReference(MyService.class);
        ServiceReference<MyService> ref2 = bundle2.getBundleContext().getServiceReference(MyService.class);
        ServiceObjects<MyService> so1 = bundle1.getBundleContext().getServiceObjects(ref1);
        ServiceObjects<MyService> so2 = bundle2.getBundleContext().getServiceObjects(ref2);
        MyService hello1 = so1.getService();
        MyService hello2 = so2.getService();

        Assert.assertNotNull(hello1);
        Assert.assertNotNull(hello2);
        Assert.assertEquals(1, Hello.CREATED.get());
        Assert.assertEquals(0, Hello.DESTROYED.get());
        Assert.assertSame(hello1, hello2);

        Assert.assertSame(hello1, so1.getService());
        Assert.assertSame(hello2, so2.getService());

        so1.ungetService(hello1);
        so2.ungetService(hello2);

        Assert.assertEquals(1, Hello.CREATED.get());
        Assert.assertEquals(0, Hello.DESTROYED.get());

    }

    public interface MyService {

    }

    @Service @Component @SingletonScoped
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
