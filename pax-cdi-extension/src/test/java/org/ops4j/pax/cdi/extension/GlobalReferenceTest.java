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

import javax.inject.Inject;

import org.junit.Test;
import org.ops4j.pax.cdi.api.Global;
import org.ops4j.pax.cdi.api.Immediate;
import org.ops4j.pax.cdi.api.Service;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GlobalReferenceTest extends AbstractTest {

    @Test
    public void test() throws Exception {
        CdiContainer container = mock(CdiContainer.class);
        CdiContainerFactory factory = mock(CdiContainerFactory.class);
        when(factory.getContainer(framework)).thenReturn(container);

        register(CdiContainerFactory.class, factory);
        createCdi(Hello.class);

        verify(container).pause();

        reset(container, factory);

        register(MyService.class, new MyService() { });

        verify(container).resume();
    }

    public interface MyService {

    }

    @Immediate
    public static class Hello {

        @Inject @Service @Global
        MyService service;

    }

}
