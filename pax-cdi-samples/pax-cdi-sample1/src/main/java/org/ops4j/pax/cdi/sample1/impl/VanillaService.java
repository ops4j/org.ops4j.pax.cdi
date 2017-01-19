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
package org.ops4j.pax.cdi.sample1.impl;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Default;

import org.ops4j.pax.cdi.api.*;
import org.ops4j.pax.cdi.sample1.IceCreamService;
import org.ops4j.pax.cdi.sample1.Vanilla;

@Vanilla
@Default
@Service @Component
@Contracts({ @Contract(VanillaService.class), @Contract(IceCreamService.class) })
@Properties(@Property(name = "flavour", value = "vanilla"))
class VanillaService implements IceCreamService {

    private boolean initialized;

    @PostConstruct
    public void init() {
        initialized = true;
    }

    @Override
    public String getFlavour() {
        if (!initialized) {
            throw new AssertionError("VanillaService is not initialized");
        }
        return "Vanilla";
    }
}
