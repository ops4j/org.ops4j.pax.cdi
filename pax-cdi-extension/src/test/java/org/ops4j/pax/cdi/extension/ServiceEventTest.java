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

import javax.enterprise.event.Observes;
import java.util.concurrent.atomic.AtomicInteger;

import org.ops4j.pax.cdi.api.event.ServiceAdded;
import org.ops4j.pax.cdi.api.event.ServiceRemoved;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;


/**
 */
public class ServiceEventTest extends AbstractTest {

    @Test
    public void test() {
        createCdi(ServiceEventReceiver.class);

        Assert.assertEquals(0, ServiceEventReceiver.added.get());
        Assert.assertEquals(0, ServiceEventReceiver.removed.get());

        ServiceRegistration<MyService> registration1 = register(MyService.class, new MyService() {
            @Override
            public String hello() {
                return "Hello 1 !!";
            }
        });

        Assert.assertEquals(1, ServiceEventReceiver.added.get());
        Assert.assertEquals(0, ServiceEventReceiver.removed.get());

        registration1.unregister();

        Assert.assertEquals(1, ServiceEventReceiver.added.get());
        Assert.assertEquals(1, ServiceEventReceiver.removed.get());
    }

    public static class ServiceEventReceiver {

        static AtomicInteger added = new AtomicInteger();
        static AtomicInteger removed = new AtomicInteger();

        public void added(@Observes @ServiceAdded MyService service) {
            System.err.println("ReferenceAdded");
            added.incrementAndGet();
        }
        public void removed(@Observes @ServiceRemoved MyService service) {
            System.err.println("ReferenceRemoved");
            removed.incrementAndGet();
        }
    }

    public interface MyService {
        String hello();
    }

}
