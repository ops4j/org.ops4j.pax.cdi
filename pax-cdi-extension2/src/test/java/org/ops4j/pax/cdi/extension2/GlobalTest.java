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

import javax.inject.Inject;

import org.easymock.EasyMock;
import org.junit.Test;
import org.ops4j.pax.cdi.api2.Global;
import org.ops4j.pax.cdi.api2.Immediate;
import org.ops4j.pax.cdi.api2.Service;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;

public class GlobalTest extends AbstractTest {

    @Test
    public void test() throws Exception {
        CdiContainer container = EasyMock.createMock(CdiContainer.class);
        CdiContainerFactory factory = EasyMock.createMock(CdiContainerFactory.class);
        EasyMock.expect(factory.getContainer(framework)).andReturn(container).anyTimes();
        container.pause();
        EasyMock.expectLastCall();
        EasyMock.replay(container, factory);

        register(CdiContainerFactory.class, factory);

        createCdi(Hello.class);

        EasyMock.verify(container, factory);

        EasyMock.reset(container, factory);
        container.resume();
        EasyMock.expectLastCall();
        EasyMock.replay(container, factory);

        register(MyService.class, new MyService() { });

        EasyMock.verify(container, factory);
    }

    public interface MyService {

    }

    @Immediate
    public static class Hello {

        @Inject @Service @Global
        MyService service;

    }

}
