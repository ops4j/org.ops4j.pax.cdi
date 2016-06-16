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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.ops4j.pax.cdi.api.BundleScoped;
import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.Service;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

public class ScopeBundleTest extends AbstractTest {

    @Test
    public void test() throws Exception {
        createCdi(Hello.class);

        Assert.assertEquals(0, Hello.created.get());
        Assert.assertEquals(0, Hello.destroyed.get());

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
        Assert.assertEquals(2, Hello.created.get());
        Assert.assertEquals(0, Hello.destroyed.get());
        Assert.assertNotSame(hello1, hello2);

        Assert.assertSame(hello1, so1.getService());
        Assert.assertSame(hello2, so2.getService());

        so1.ungetService(hello1);
        so2.ungetService(hello2);

        Assert.assertEquals(2, Hello.created.get());
        Assert.assertEquals(0, Hello.destroyed.get());

        so1.ungetService(hello1);
        so2.ungetService(hello2);

        Assert.assertEquals(2, Hello.created.get());
        Assert.assertEquals(2, Hello.destroyed.get());
    }

    public interface MyService {

    }

    @Service @Component @BundleScoped
    public static class Hello implements MyService {

        static final AtomicInteger created = new AtomicInteger();
        static final AtomicInteger destroyed = new AtomicInteger();
        static final AtomicReference<Hello> instance = new AtomicReference<>();

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
            return "Hello world !!";
        }
    }

}
