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

import java.lang.annotation.Retention;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Qualifier;

import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.cdi.api.Attribute;
import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.Immediate;
import org.ops4j.pax.cdi.api.Service;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class FilterAttributeTest extends AbstractTest {

    @Test
    public void test() throws Exception {
        createCdi("provider", Provider.class);
        createCdi("consumer", Consumer.class);

        Consumer consumer = Consumer.INSTANCE.get();
        Provider provider = Provider.INSTANCE.get();

        Assert.assertNotNull(consumer);
        Assert.assertNotNull(provider);
        Assert.assertNotNull(consumer.provider);
        Assert.assertSame(provider, consumer.provider);
        Assert.assertSame(provider, getService(Provider.class, "(myattribute=1)"));

    }

    @Attribute("myattribute")
    @Retention(RUNTIME) @Qualifier
    public @interface MyAttribute {
        int value();
    }

    @Immediate @Component
    public static class Consumer {

        static final AtomicReference<Consumer> INSTANCE = new AtomicReference<>();

        @Inject @Service @MyAttribute(1)
        Provider provider;

        @PostConstruct
        public void init() {
            INSTANCE.set(this);
        }

    }

    @Service @Component @MyAttribute(1)
    public static class Provider {

        static final AtomicReference<Provider> INSTANCE = new AtomicReference<>();

        @PostConstruct
        public void init() {
            INSTANCE.set(this);
        }

    }

}
