/*
 * Copyright 2012 Harald Wellmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.sample1.client;

import javax.inject.Inject;

import org.ops4j.pax.cdi.api.Timeout;
import org.ops4j.pax.cdi.sample1.IceCreamService;
import org.osgi.service.cdi.Component;
import org.osgi.service.cdi.Filter;
import org.osgi.service.cdi.Service;

@Component
public class StrawberryClient {

    @Inject
    @Service
    @Filter("flavour=strawberry")
    @Timeout(2000)
    private IceCreamService iceCreamService;

    public String getFlavour() {
        return iceCreamService.getFlavour();
    }

}
